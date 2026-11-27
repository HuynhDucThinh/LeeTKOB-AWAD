/**
 * Automated End-to-End Smoke Test for PR 3A (Stripper + Syntax-Aware Hard Rules)
 * and Section 0 JudgeWorker Integration Fixes (Syllabus Rule, Sampler, Publish & Appeal).
 *
 * Usage (with backend already running on port 8080):
 *   node scripts/test-pr3a-e2e.mjs
 * Or against custom URL:
 *   BACKEND_URL=http://127.0.0.1:8086 node scripts/test-pr3a-e2e.mjs
 */

const BASE_URL = (process.env.BACKEND_URL || 'http://127.0.0.1:8080').replace(/\/$/, '');

function assert(condition, message) {
  if (!condition) {
    throw new Error(`ASSERTION FAILED: ${message}`);
  }
}

async function api(method, path, token, body) {
  const headers = {};
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = text;
  }
  return { status: res.status, data: json };
}

async function login(email, password) {
  const res = await api('POST', '/api/auth/login', null, { email, password });
  assert(res.status === 200 && res.data?.data?.accessToken, `Login failed for ${email}: ${res.status}`);
  return res.data.data.accessToken;
}

async function waitForJudged(submissionId, token, timeoutMs = 15000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const res = await api('GET', `/v1/submissions/${submissionId}`, token);
    assert(res.status === 200, `GET /v1/submissions/${submissionId} returned ${res.status}`);
    const status = res.data.judgeStatus;
    if (status !== 'queued' && status !== 'running') {
      return res.data;
    }
    await new Promise((r) => setTimeout(r, 250));
  }
  throw new Error(`Timed out waiting for submission ${submissionId} to finish judging`);
}

async function main() {
  console.log(`\n=== Running PR 3A + Section 0 Live E2E Test against ${BASE_URL} ===\n`);

  // 1. Authenticate demo accounts
  const [ta1Token, ta2Token, sv1Token, sv2Token] = await Promise.all([
    login('ta1@leetkob.edu', 'Ta1@1234'),
    login('ta2@leetkob.edu', 'Ta2@1234'),
    login('student1@leetkob.edu', 'Student1@123'),
    login('student2@leetkob.edu', 'Student2@123'),
  ]);
  console.log('[PASS] 1. Logged in TA1, TA2, Student1, Student2');

  // 2. TA1 creates Lab with Hard Ban-List Constraint
  const futureDeadline = new Date(Date.now() + 3600_000).toISOString();
  const labPayload = {
    classId: 10,
    title: `PR3A Auto-Test Sort Lab (${Date.now()})`,
    deadline: futureDeadline,
    costCapUsd: 5.0,
    statementMd: '# Sort 3 integers in ascending order without calling std::sort.',
    tests: [
      {
        name: 'Sample 1',
        input: '3\n3 1 2\n',
        expectedOutput: '1 2 3',
        timeLimitMs: 2000,
        memoryLimitMb: 256,
        isSample: true,
      },
    ],
    constraints: [
      {
        constraintId: 'C1',
        type: 'hard',
        description: 'Do not call `std::sort` or `std::stable_sort`.',
        deductionTenths: 30,
      },
    ],
  };

  const createLabRes = await api('POST', '/v1/labs', ta1Token, labPayload);
  assert(createLabRes.status === 201, `Create lab expected 201, got ${createLabRes.status}`);
  const labId = createLabRes.data.id;

  const foreignCheck = await api('GET', `/v1/labs/${labId}`, ta2Token);
  assert(foreignCheck.status === 404, `Foreign TA2 expected 404 on lab ${labId}, got ${foreignCheck.status}`);
  console.log(`[PASS] 2. TA1 created Lab #${labId}; Foreign TA2 isolated with 404 (T1)`);

  // 3. Student 1 submits C++ code with comments, PII, and banned std::sort on line 6
  const violationCode = [
    '// Author: Student 1 (student1@leetkob.edu)',
    '#include <algorithm>',
    '#include <iostream>',
    'int main() {',
    '  int a[3] = {3, 1, 2};',
    '  std::sort(a, a + 3);',
    '  std::cout << a[0] << " " << a[1] << " " << a[2];',
    '  return 0;',
    '}',
  ].join('\n');

  const sub1Create = await api('POST', `/v1/labs/${labId}/submissions`, sv1Token, {
    sourceCode: violationCode,
  });
  assert(sub1Create.status === 201, `Student1 submit expected 201, got ${sub1Create.status}`);
  const sub1Judged = await waitForJudged(sub1Create.data.id, sv1Token);

  assert(sub1Judged.judgeStatus === 'AC', `Expected AC for sub1, got ${sub1Judged.judgeStatus}`);
  assert(
    sub1Judged.scoreTenths === 100,
    `Syllabus Rule check: expected functional scoreTenths=100 (NO pre-TA auto deduction!), got ${sub1Judged.scoreTenths}`,
  );
  assert(sub1Judged.publishedScoreTenths === null, 'Expected publishedScoreTenths=null before publish');
  assert(sub1Judged.publishedDeductions === null, 'Expected publishedDeductions=null before publish');
  console.log(
    `[PASS] 3. Student1 submission #${sub1Judged.id} judged AC; functional score=100, publishedScore=null (Zero pre-TA deduction!)`,
  );

  // 4. Student 2 submits Trap Solution (<algorithm> for std::swap, std::sort in comment/string, custom namespace & nested myalgo::std::sort)
  const trapCode = [
    '#include <algorithm> // only for std::swap, do not flag std::sort here',
    '#include <iostream>',
    '/* Block comment mentioning std::stable_sort(a, a + 3); */',
    'namespace myalgo {',
    '  void sort(int* a) { if (a[0] > a[1]) std::swap(a[0], a[1]); }',
    '  namespace std { void sort(int* a) {} }',
    '}',
    'int main() {',
    '  const char* msg = "std::sort(a, a + 3)";',
    '  (void)msg;',
    '  int a[3] = {2, 1, 3};',
    '  myalgo::sort(a);',
    '  myalgo::std::sort(a);',
    '  std::cout << "1 2 3";',
    '  return 0;',
    '}',
  ].join('\n');

  const sub2Create = await api('POST', `/v1/labs/${labId}/submissions`, sv2Token, {
    sourceCode: trapCode,
  });
  assert(sub2Create.status === 201, `Student2 submit expected 201, got ${sub2Create.status}`);
  const sub2Judged = await waitForJudged(sub2Create.data.id, sv2Token);
  assert(sub2Judged.judgeStatus === 'AC', `Expected AC for sub2, got ${sub2Judged.judgeStatus}`);
  console.log(`[PASS] 4. Student2 trap submission #${sub2Judged.id} judged AC`);

  // 5. TA1 inspects Review Queue: checks Stripper line map, PII redaction, exact evidence line 6, and 0 trap false-positive
  const queueRes = await api('GET', `/v1/labs/${labId}/review-queue`, ta1Token);
  assert(queueRes.status === 200, `GET review-queue expected 200, got ${queueRes.status}`);
  const items = queueRes.data.items;

  const sub1Item = items.find((it) => it.submissionId === sub1Judged.id && it.constraintId === 'C1');
  assert(sub1Item, 'Expected Student1 violation to appear in TA Review Queue');
  assert(sub1Item.verdict === 'violation', `Expected verdict=violation, got ${sub1Item.verdict}`);
  assert(sub1Item.source === 'hard_rule', `Expected source=hard_rule, got ${sub1Item.source}`);
  assert(
    Array.isArray(sub1Item.evidence) &&
      sub1Item.evidence.length === 1 &&
      sub1Item.evidence[0].line === 6 &&
      sub1Item.evidence[0].text === 'std::sort(a, a + 3);',
    `Expected exact line 6 evidence 'std::sort(a, a + 3);', got ${JSON.stringify(sub1Item.evidence)}`,
  );
  assert(
    !sub1Item.strippedCode.toLowerCase().includes('student1@leetkob.edu'),
    'Expected student email stripped from strippedCode',
  );
  assert(
    sub1Item.strippedCode.split('\n').length === violationCode.split('\n').length,
    'Expected 1-to-1 line count preserved in strippedCode',
  );

  const sub2Item = items.find((it) => it.submissionId === sub2Judged.id && it.constraintId === 'C1');
  if (sub2Item) {
    assert(
      sub2Item.verdict === 'pass' && sub2Item.sampledPass === true,
      `Trap submission MUST NOT be flagged violation! Got: ${JSON.stringify(sub2Item)}`,
    );
  }
  console.log(
    '[PASS] 5. Review Queue verified: C1 violation on exact line 6, comments & PII stripped 1:1, Trap solution NOT flagged as violation',
  );

  // 6. TA1 decides all queued items (confirms Student1 violation; approves Student2 if 10%-sampled)
  const decide1 = await api(
    'PUT',
    `/v1/labs/${labId}/review-queue/${sub1Judged.id}/C1`,
    ta1Token,
    { decision: 'violation' },
  );
  assert(decide1.status === 200, `TA decide sub1 expected 200, got ${decide1.status}`);
  if (sub2Item) {
    const decide2 = await api(
      'PUT',
      `/v1/labs/${labId}/review-queue/${sub2Judged.id}/C1`,
      ta1Token,
      { decision: 'pass' },
    );
    assert(decide2.status === 200, `TA decide sampled sub2 expected 200, got ${decide2.status}`);
  }
  console.log('[PASS] 6. TA1 reviewed and confirmed decisions in Review Queue');

  // 7. Move deadline to past and Publish Marks
  const pastDeadline = new Date(Date.now() - 60_000).toISOString();
  const updateLabRes = await api('PUT', `/v1/labs/${labId}`, ta1Token, {
    ...labPayload,
    deadline: pastDeadline,
  });
  assert(updateLabRes.status === 200, `Update lab deadline expected 200, got ${updateLabRes.status}`);

  const publishRes = await api('POST', `/v1/labs/${labId}/publish`, ta1Token, {});
  assert(publishRes.status === 200, `Publish expected 200, got ${publishRes.status}: ${JSON.stringify(publishRes.data)}`);
  assert(publishRes.data.submissionCount === 2, `Expected submissionCount=2, got ${publishRes.data.submissionCount}`);
  console.log('[PASS] 7. TA1 published Lab marks atomically (200 OK)');

  // 8. Verify post-publish marks & deductions visible to students
  const sub1AfterPub = await api('GET', `/v1/submissions/${sub1Judged.id}`, sv1Token);
  const sub2AfterPub = await api('GET', `/v1/submissions/${sub2Judged.id}`, sv2Token);
  assert(
    sub1AfterPub.data.publishedScoreTenths === 70,
    `Expected Student1 publishedScoreTenths=70 (100 - 30), got ${sub1AfterPub.data.publishedScoreTenths}`,
  );
  assert(
    sub1AfterPub.data.publishedDeductions?.length === 1 &&
      sub1AfterPub.data.publishedDeductions[0].deductionTenths === 30,
    `Expected 1 deduction of 30 tenths for Student1, got ${JSON.stringify(sub1AfterPub.data.publishedDeductions)}`,
  );
  assert(
    sub2AfterPub.data.publishedScoreTenths === 100 &&
      sub2AfterPub.data.publishedDeductions?.length === 0,
    `Expected Student2 publishedScoreTenths=100 with 0 deductions, got ${JSON.stringify(sub2AfterPub.data)}`,
  );
  console.log('[PASS] 8. Post-publish scores verified: Student1 = 7.0/10 (-3.0 on C1), Student2 = 10.0/10');

  // 9. Student 1 appeals C1 -> TA1 accepts -> Published score restored to 100
  const appealRes = await api('POST', `/v1/submissions/${sub1Judged.id}/appeals`, sv1Token, {
    constraintId: 'C1',
    reason: 'Testing appeal score restoration flow',
  });
  assert(appealRes.status === 201, `Create appeal expected 201, got ${appealRes.status}`);

  const acceptAppealRes = await api('PUT', `/v1/appeals/${appealRes.data.id}/decision`, ta1Token, {
    status: 'accepted',
    decisionReason: 'Approved in E2E test',
  });
  assert(acceptAppealRes.status === 200, `Accept appeal expected 200, got ${acceptAppealRes.status}`);

  const sub1AfterAppeal = await api('GET', `/v1/submissions/${sub1Judged.id}`, sv1Token);
  assert(
    sub1AfterAppeal.data.publishedScoreTenths === 100,
    `Expected Student1 publishedScoreTenths=100 after accepted appeal, got ${sub1AfterAppeal.data.publishedScoreTenths}`,
  );
  console.log('[PASS] 9. Appeal accepted by TA1 -> Student1 published score restored to 10.0/10');

  console.log('\nALL 9 END-TO-END CHECKS PASSED SUCCESSFULLY!\n');
}

main().catch((err) => {
  console.error('\n[FAIL]', err.message);
  process.exit(1);
});
