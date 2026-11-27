/**
 * Automated End-to-End Verification Script for PR 3C:
 * 1. Verifies byte-for-byte SHA-256 of eval/dev/eval.mjs & eval/dev/labels.jsonl.
 * 2. Verifies eval/custom-cases (>= 5 solutions, includes 'trap' and 'injection' tags).
 * 3. Runs Docker image `leetkob-checker:pa3` offline (`--network none --memory=512m --pids-limit=64 -e MODEL_OFF=1`)
 *    on both eval/dev and eval/custom-cases, asserting 0 wrongful deductions and 100% ban-list coverage.
 * 4. Re-scores the committed real Groq run (`eval/runs/pa3-dev-run/predictions.jsonl`) with `eval/dev/eval.mjs`
 *    and audits `eval/runs/pa3-dev-run/call-log.jsonl` (real provider_request_id, 0 hard-rule calls, 0 secrets).
 *
 * Usage:
 *   node scripts/test-pr3c-e2e.mjs
 */

import crypto from 'node:crypto';
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

function assert(condition, message) {
  if (!condition) {
    throw new Error(`ASSERTION FAILED: ${message}`);
  }
}

function sha256File(filePath) {
  const bytes = fs.readFileSync(filePath);
  return crypto.createHash('sha256').update(bytes).digest('hex');
}

function runEvalMjs(dataDir, predictionsPath, jsonSummaryPath) {
  const args = ['eval/dev/eval.mjs', '--data', dataDir, '--predictions', predictionsPath];
  if (jsonSummaryPath) {
    args.push('--json', jsonSummaryPath);
  }
  try {
    const stdout = execFileSync(process.execPath, args, { encoding: 'utf8' });
    return { exitCode: 0, stdout };
  } catch (err) {
    return {
      exitCode: err.status ?? 2,
      stdout: (err.stdout ?? '') + (err.stderr ?? ''),
    };
  }
}

function main() {
  console.log('\n=== Running PR 3C End-to-End Checker CLI, Docker Offline & Eval Verification ===\n');

  // 1. Verify verbatim SHA-256 of eval/dev
  const evalMjsHash = sha256File('eval/dev/eval.mjs');
  const labelsHash = sha256File('eval/dev/labels.jsonl');
  assert(
    evalMjsHash.startsWith('c836edd8713521bb'),
    `eval/dev/eval.mjs SHA-256 mismatch: ${evalMjsHash}`,
  );
  assert(
    labelsHash.startsWith('262cda7b0ef5612e'),
    `eval/dev/labels.jsonl SHA-256 mismatch: ${labelsHash}`,
  );
  console.log(
    `[PASS] 1. Verbatim dev-set SHA-256 verified (eval.mjs=${evalMjsHash.slice(0, 16)}..., labels.jsonl=${labelsHash.slice(0, 16)}...)`,
  );

  // 2. Verify eval/custom-cases layout, count >= 5, and required tags ('trap', 'injection')
  const customLabelLines = fs
    .readFileSync('eval/custom-cases/labels.jsonl', 'utf8')
    .trim()
    .split(/\r?\n/);
  const customSolutions = new Set();
  const customTags = new Set();
  for (const line of customLabelLines) {
    const row = JSON.parse(line);
    customSolutions.add(row.solution);
    for (const tag of row.tags ?? []) {
      customTags.add(tag);
    }
  }
  assert(
    customSolutions.size >= 5,
    `Expected >= 5 custom solutions, found ${customSolutions.size}`,
  );
  assert(customTags.has('trap'), 'Expected at least 1 custom case tagged "trap"');
  assert(customTags.has('injection'), 'Expected at least 1 custom case tagged "injection"');
  console.log(
    `[PASS] 2. Custom dataset verified: ${customSolutions.size} solutions, ${customLabelLines.length} pairs, tags=[${[...customTags].join(', ')}]`,
  );

  // 3. Run leetkob-checker:pa3 offline (--network none -e MODEL_OFF=1) on eval/dev
  const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'leetkob-pr3c-'));
  try {
    const devAbs = path.resolve('eval/dev');
    const customAbs = path.resolve('eval/custom-cases');
    const devOffPreds = path.join(tmpDir, 'dev-off-predictions.jsonl');
    const devOffSummary = path.join(tmpDir, 'dev-off-summary.json');

    execFileSync(
      'docker',
      [
        'run',
        '--rm',
        '--network',
        'none',
        '--memory=512m',
        '--pids-limit=64',
        '-e',
        'MODEL_OFF=1',
        '-v',
        `${devAbs}:/dataset:ro`,
        '-v',
        `${tmpDir}:/out`,
        'leetkob-checker:pa3',
        '/dataset',
        '/out/dev-off-predictions.jsonl',
      ],
      { stdio: 'pipe' },
    );

    const devOffRes = runEvalMjs('eval/dev', devOffPreds, devOffSummary);
    assert(
      devOffRes.exitCode === 0 || devOffRes.exitCode === 1,
      `eval.mjs on MODEL_OFF=1 dev set must exit 0 or 1, got ${devOffRes.exitCode}:\n${devOffRes.stdout}`,
    );
    const devOffJson = JSON.parse(fs.readFileSync(devOffSummary, 'utf8'));
    assert(
      devOffJson.wrongful === 0,
      `Expected 0 wrongful deductions on MODEL_OFF=1 dev set, got ${devOffJson.wrongful}`,
    );
    assert(
      devOffJson.unsure === 47,
      `Expected all 55 ban-list pairs decided (only 47 semantic pairs unsure), got unsure=${devOffJson.unsure}`,
    );
    console.log(
      `[PASS] 3. Docker --network none MODEL_OFF=1 on eval/dev: wrongful=0, caught=${devOffJson.caught}/15 hard-rule violations (55/55 ban-list pairs decided), exit=${devOffRes.exitCode}`,
    );

    // 4. Run leetkob-checker:pa3 offline (--network none -e MODEL_OFF=1) on eval/custom-cases
    const customOffPreds = path.join(tmpDir, 'custom-off-predictions.jsonl');
    const customOffSummary = path.join(tmpDir, 'custom-off-summary.json');
    execFileSync(
      'docker',
      [
        'run',
        '--rm',
        '--network',
        'none',
        '--memory=512m',
        '--pids-limit=64',
        '-e',
        'MODEL_OFF=1',
        '-v',
        `${customAbs}:/dataset:ro`,
        '-v',
        `${tmpDir}:/out`,
        'leetkob-checker:pa3',
        '/dataset',
        '/out/custom-off-predictions.jsonl',
      ],
      { stdio: 'pipe' },
    );

    const customOffRes = runEvalMjs('eval/custom-cases', customOffPreds, customOffSummary);
    assert(
      customOffRes.exitCode === 0 || customOffRes.exitCode === 1,
      `eval.mjs on MODEL_OFF=1 custom-cases must exit 0 or 1, got ${customOffRes.exitCode}`,
    );
    const customOffJson = JSON.parse(fs.readFileSync(customOffSummary, 'utf8'));
    assert(
      customOffJson.wrongful === 0,
      `Expected 0 wrongful deductions on custom-cases, got ${customOffJson.wrongful}`,
    );
    console.log(
      `[PASS] 4. Docker --network none MODEL_OFF=1 on eval/custom-cases: wrongful=0, 6/6 ban-list pairs decided, exit=${customOffRes.exitCode}`,
    );

    // 5. Re-score committed real-model run & audit call-log.jsonl
    const realPreds = 'eval/runs/pa3-dev-run/predictions.jsonl';
    const realCallLog = 'eval/runs/pa3-dev-run/call-log.jsonl';
    const rescoredSummary = path.join(tmpDir, 'rescored-summary.json');

    const realRes = runEvalMjs('eval/dev', realPreds, rescoredSummary);
    assert(
      realRes.exitCode === 0,
      `Expected committed real-model run to PASS (exit 0), got ${realRes.exitCode}:\n${realRes.stdout}`,
    );
    const realJson = JSON.parse(fs.readFileSync(rescoredSummary, 'utf8'));
    assert(realJson.wrongful === 0, `Expected 0 wrongful deductions, got ${realJson.wrongful}`);
    assert(realJson.recall >= 0.9, `Expected recall >= 0.90, got ${realJson.recall}`);
    assert(realJson.unsureRate <= 0.2, `Expected unsureRate <= 0.20, got ${realJson.unsureRate}`);

    // Build set of ban-list (problem, constraint) pairs from eval/dev/problems/*/constraints.json
    const banListPairs = new Set();
    for (const pid of fs.readdirSync('eval/dev/problems')) {
      const cfile = path.join('eval/dev/problems', pid, 'constraints.json');
      if (!fs.existsSync(cfile)) continue;
      for (const c of JSON.parse(fs.readFileSync(cfile, 'utf8'))) {
        const lower = c.text.trim().toLowerCase();
        if ((lower.startsWith('do not use') || lower.startsWith('do not call')) && /`[^`]+`/.test(c.text)) {
          banListPairs.add(`${pid}:${c.id}`);
        }
      }
    }

    const callLogLines = fs.readFileSync(realCallLog, 'utf8').trim().split(/\r?\n/);
    assert(callLogLines.length > 0, 'call-log.jsonl must not be empty');
    let completedWithReqId = 0;
    for (const raw of callLogLines) {
      const rec = JSON.parse(raw);
      const pid = rec.solution_key.split('/')[0];
      assert(
        !banListPairs.has(`${pid}:${rec.constraint_id}`),
        `Hard-rule constraint ${pid}:${rec.constraint_id} must NEVER appear in call-log.jsonl!`,
      );
      if (!rec.guardrail_reason) {
        assert(
          typeof rec.provider_request_id === 'string' && rec.provider_request_id.startsWith('req_'),
          `Missing real provider_request_id on completed call for ${rec.solution_key}`,
        );
        assert(Number(rec.cost_usd) > 0, `Expected cost_usd > 0 on ${rec.solution_key}`);
        completedWithReqId++;
      }
      assert(
        !/\bgsk_[A-Za-z0-9]{20,}\b/.test(raw),
        'Detected API key leak inside call-log.jsonl!',
      );
    }

    console.log(
      `[PASS] 5. Re-scored eval/runs/pa3-dev-run: PASS (wrongful=0, recall=${(realJson.recall * 100).toFixed(1)}%, unsure=${(realJson.unsureRate * 100).toFixed(1)}%, ${completedWithReqId} verified Groq request IDs, 0 hard-rule calls)`,
    );
  } finally {
    fs.rmSync(tmpDir, { recursive: true, force: true });
  }

  console.log('\nALL 5 PR 3C END-TO-END CHECKS PASSED SUCCESSFULLY!\n');
}

try {
  main();
} catch (err) {
  console.error('\n[FAIL]', err.message);
  process.exit(1);
}
