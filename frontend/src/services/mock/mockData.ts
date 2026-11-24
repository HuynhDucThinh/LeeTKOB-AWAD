import type { AuthUser } from '@/types/auth'
import type { ClassRoom } from '@/types/admin'
import type { LabDetail } from '@/types/lab'
import type { ReviewItem } from '@/types/review'
import type { SubmissionDetail } from '@/types/submission'
import type { AppealItem } from '@/types/appeal'

export const mockUsers: AuthUser[] = [
  { id: 'admin-1', username: 'admin', displayName: 'System Admin', role: 'admin', classIds: [], createdAt: '2026-09-01T07:00:00.000Z' },
  { id: 'ta-own', username: 'ta1', displayName: 'Nguyễn Minh', role: 'ta', classIds: [10, 12], createdAt: '2026-09-02T07:00:00.000Z' },
  { id: 'ta-foreign', username: 'ta2', displayName: 'Trần Bảo', role: 'ta', classIds: [20], createdAt: '2026-09-02T07:00:00.000Z' },
  { id: 'student-1', username: '23120197', displayName: 'Trà Văn Sỹ', role: 'student', classIds: [10, 12], createdAt: '2026-09-03T07:00:00.000Z' },
  { id: 'student-2', username: '23120228', displayName: 'Lê Trung Thành Đạt', role: 'student', classIds: [10], createdAt: '2026-09-03T07:00:00.000Z' },
]

export const mockClasses: ClassRoom[] = [
  { id: 10, name: 'CS101 - Merge Sort & Recursion', taIds: ['ta-own'], studentIds: ['student-1', 'student-2'], labCount: 2 },
  { id: 12, name: 'CS102 - Data Structures', taIds: ['ta-own'], studentIds: ['student-1'], labCount: 1 },
  { id: 20, name: 'CS101 - Class 20 (Foreign TA)', taIds: ['ta-foreign'], studentIds: [], labCount: 1 },
]

export const mockLabs: LabDetail[] = [
  {
    id: 1, classId: 10, className: 'CS101 - Merge Sort & Recursion', title: 'Lab 01: Merge Sort',
    deadline: '2026-11-05T08:00:00.000Z', costCapUsd: 1.5, costSpentUsd: 0.18, isPublished: false,
    publishedAt: null, submissionCount: 2, judgingPendingCount: 0, unreviewedCount: 3,
    statementMd: 'Implement Merge Sort to sort N integers in ascending order. N can be up to 100,000.',
    tests: [{ id: 1, name: 'Sample 1', input: '5\n5 2 4 1 3', expectedOutput: '1 2 3 4 5', timeLimitMs: 2000, memoryLimitMb: 256, isSample: true }],
    constraints: [
      { constraintId: 'C1', type: 'hard', description: 'Do not use `std::sort` or `std::stable_sort`.', deductionTenths: 50 },
      { constraintId: 'C2', type: 'semantic', description: 'Must implement recursive Merge Sort with a merge step.', deductionTenths: 30 },
    ],
    publicationReceipt: null,
  },
  {
    id: 2, classId: 10, className: 'CS101 - Merge Sort & Recursion', title: 'Lab 02: Binary Search',
    deadline: '2026-12-10T08:00:00.000Z', costCapUsd: 1.2, costSpentUsd: 0.04, isPublished: true,
    publishedAt: '2026-12-10T10:30:00.000Z', submissionCount: 1, judgingPendingCount: 0, unreviewedCount: 0,
    statementMd: 'Implement iterative binary search on a sorted integer array.',
    tests: [{ id: 2, name: 'Sample', input: '5 4\n1 2 4 8 16', expectedOutput: '2', timeLimitMs: 1000, memoryLimitMb: 128, isSample: true }],
    constraints: [{ constraintId: 'C1', type: 'semantic', description: 'The solution must use binary search.', deductionTenths: 40 }],
    publicationReceipt: { labId: 2, status: 'published', publishedAt: '2026-12-10T10:30:00.000Z', publishedBy: 'ta-own', submissionCount: 1 },
  },
  {
    id: 3, classId: 12, className: 'CS102 - Data Structures', title: 'Lab 01: Singly Linked List',
    deadline: '2026-12-18T08:00:00.000Z', costCapUsd: 1.4, costSpentUsd: 0.07, isPublished: false,
    publishedAt: null, submissionCount: 1, judgingPendingCount: 0, unreviewedCount: 0,
    statementMd: 'Implement a singly linked list with insert, erase, find, and traversal operations.',
    tests: [{ id: 3, name: 'Basic operations', input: '5\nI 3\nI 7\nF 3\nE 3\nF 3', expectedOutput: 'YES\nNO', timeLimitMs: 1500, memoryLimitMb: 256, isSample: true }],
    constraints: [
      { constraintId: 'C1', type: 'hard', description: 'Do not use `std::list` or `std::forward_list`.', deductionTenths: 50 },
      { constraintId: 'C2', type: 'semantic', description: 'Must maintain links using a student-defined Node structure.', deductionTenths: 30 },
    ],
    publicationReceipt: null,
  },
  {
    id: 4, classId: 20, className: 'CS101 - Class 20 (Foreign TA)', title: 'Lab 01: Heap Sort',
    deadline: '2026-12-22T08:00:00.000Z', costCapUsd: 1.1, costSpentUsd: 0, isPublished: false,
    publishedAt: null, submissionCount: 0, judgingPendingCount: 0, unreviewedCount: 0,
    statementMd: 'Implement Heap Sort without calling a library sorting function.',
    tests: [{ id: 4, name: 'Sample', input: '4\n9 1 7 3', expectedOutput: '1 3 7 9', timeLimitMs: 1500, memoryLimitMb: 256, isSample: true }],
    constraints: [{ constraintId: 'C1', type: 'hard', description: 'Do not use `std::sort`.', deductionTenths: 50 }],
    publicationReceipt: null,
  },
]

const bubbleCode = `#include <algorithm>
#include <iostream>
#include <vector>
void mergeSort(std::vector<int>& a, int n) {
  for (int i = 0; i < n; ++i)
    for (int j = 0; j + 1 < n - i; ++j)
      if (a[j] > a[j+1]) std::swap(a[j], a[j+1]);
}`

export const mockReviewItems: ReviewItem[] = [
  { submissionId: 32, labId: 1, studentId: 'student-2', constraintId: 'C1', constraintType: 'hard', constraintDescription: 'Do not use `std::sort` or `std::stable_sort`.', deductionTenths: 50, verdict: 'violation', source: 'hard_rule', evidence: [{ line: 7, text: 'std::sort(a.begin(), a.end());' }], guardrailReason: null, sampledPass: false, decision: null, decidedBy: null, decidedAt: null, sourceCode: '#include <algorithm>\n#include <iostream>\n#include <vector>\nint main() {\n  int n; std::cin >> n;\n  std::vector<int> a(n);\n  std::sort(a.begin(), a.end());\n}', strippedCode: '#include <algorithm>\n#include <iostream>\n#include <vector>\nint main() {\n  int n; std::cin >> n;\n  std::vector<int> a(n);\n  std::sort(a.begin(), a.end());\n}' },
  { submissionId: 32, labId: 1, studentId: 'student-2', constraintId: 'C2', constraintType: 'semantic', constraintDescription: 'Must implement recursive Merge Sort with a merge step.', deductionTenths: 30, verdict: 'violation', source: 'llm', evidence: [{ line: 5, text: 'for (int i = 0; i < n; ++i)' }, { line: 6, text: 'for (int j = 0; j + 1 < n - i; ++j)' }], guardrailReason: null, sampledPass: false, decision: null, decidedBy: null, decidedAt: null, sourceCode: bubbleCode, strippedCode: bubbleCode },
  { submissionId: 31, labId: 1, studentId: 'student-1', constraintId: 'C1', constraintType: 'hard', constraintDescription: 'Do not use `std::sort` or `std::stable_sort`.', deductionTenths: 50, verdict: 'unsure', source: 'guardrail', evidence: [], guardrailReason: 'evidence_mismatch', sampledPass: false, decision: null, decidedBy: null, decidedAt: null, sourceCode: '// no banned library calls\n#include <vector>\nvoid mergeSort(std::vector<int>& a) { }', strippedCode: '#include <vector>\nvoid mergeSort(std::vector<int>& a) { }' },
  { submissionId: 31, labId: 1, studentId: 'student-1', constraintId: 'C2', constraintType: 'semantic', constraintDescription: 'Must implement recursive Merge Sort with a merge step.', deductionTenths: 30, verdict: 'pass', source: 'llm', evidence: [{ line: 6, text: 'mergeSort(a, l, m);' }], guardrailReason: null, sampledPass: true, decision: 'pass', decidedBy: 'ta-own', decidedAt: '2026-11-05T08:10:00.000Z', sourceCode: 'void mergeSort(vector<int>& a, int l, int r) {\n  if (l >= r) return;\n  int m = (l + r) / 2;\n  mergeSort(a, l, m);\n  mergeSort(a, m + 1, r);\n  merge(a, l, m, r);\n}', strippedCode: 'void mergeSort(vector<int>& a, int l, int r) {\n  if (l >= r) return;\n  int m = (l + r) / 2;\n  mergeSort(a, l, m);\n  mergeSort(a, m + 1, r);\n  merge(a, l, m, r);\n}' },
]

export const mockSubmissions: SubmissionDetail[] = [
  { id: 29, labId: 1, studentId: 'student-1', studentName: 'Trà Văn Sỹ', judgeStatus: 'CE', scoreTenths: 0, publishedScoreTenths: null, isCounted: false, passedTests: 0, totalTests: 5, executionTimeMs: null, memoryKb: null, compileError: 'error: expected ; before return', submittedAt: '2026-11-05T06:42:00.000Z', judgedAt: '2026-11-05T06:42:05.000Z', traceId: 'tr_1c01aa2', sourceCode: 'int main() { return 0 }', strippedCode: 'int main() { return 0 }', publishedDeductions: null },
  { id: 30, labId: 1, studentId: 'student-1', studentName: 'Trà Văn Sỹ', judgeStatus: 'WA', scoreTenths: 40, publishedScoreTenths: null, isCounted: false, passedTests: 2, totalTests: 5, executionTimeMs: 35, memoryKb: 11800, compileError: null, submittedAt: '2026-11-05T07:02:00.000Z', judgedAt: '2026-11-05T07:03:00.000Z', traceId: 'tr_3d8bb91', sourceCode: 'int main() { /* wrong output */ }', strippedCode: 'int main() { }', publishedDeductions: null },
  { id: 31, labId: 1, studentId: 'student-1', studentName: 'Trà Văn Sỹ', judgeStatus: 'AC', scoreTenths: 100, publishedScoreTenths: null, isCounted: true, passedTests: 5, totalTests: 5, executionTimeMs: 42, memoryKb: 12698, compileError: null, submittedAt: '2026-11-05T07:21:00.000Z', judgedAt: '2026-11-05T07:22:00.000Z', traceId: 'tr_8f21c72', sourceCode: 'void mergeSort(vector<int>& a, int l, int r) {\n  if (l >= r) return;\n  int m = (l + r) / 2;\n  mergeSort(a, l, m);\n  mergeSort(a, m + 1, r);\n  merge(a, l, m, r);\n}', strippedCode: 'void mergeSort(vector<int>& a, int l, int r) {\n  if (l >= r) return;\n  int m = (l + r) / 2;\n  mergeSort(a, l, m);\n  mergeSort(a, m + 1, r);\n  merge(a, l, m, r);\n}', publishedDeductions: null },
  { id: 32, labId: 1, studentId: 'student-2', studentName: 'Lê Trung Thành Đạt', judgeStatus: 'AC', scoreTenths: 100, publishedScoreTenths: null, isCounted: true, passedTests: 5, totalTests: 5, executionTimeMs: 38, memoryKb: 11980, compileError: null, submittedAt: '2026-11-05T07:40:00.000Z', judgedAt: '2026-11-05T07:41:00.000Z', traceId: 'tr_7a42e11', sourceCode: bubbleCode, strippedCode: bubbleCode, publishedDeductions: null },
  { id: 45, labId: 2, studentId: 'student-1', studentName: 'Trà Văn Sỹ', judgeStatus: 'AC', scoreTenths: 100, publishedScoreTenths: 60, isCounted: true, passedTests: 5, totalTests: 5, executionTimeMs: 24, memoryKb: 10240, compileError: null, submittedAt: '2026-12-10T07:10:00.000Z', judgedAt: '2026-12-10T07:11:00.000Z', traceId: 'tr_93ca120', sourceCode: 'int binarySearch(int a[], int n, int x) { return -1; }', strippedCode: 'int binarySearch(int a[], int n, int x) { return -1; }', publishedDeductions: [{ constraintId: 'C1', description: 'The solution must use binary search.', deductionTenths: 40, evidence: [{ line: 1, text: 'int binarySearch(int a[], int n, int x) { return -1; }' }] }] },
  { id: 50, labId: 3, studentId: 'student-1', studentName: 'Trà Văn Sỹ', judgeStatus: 'AC', scoreTenths: 90, publishedScoreTenths: null, isCounted: true, passedTests: 9, totalTests: 10, executionTimeMs: 31, memoryKb: 14200, compileError: null, submittedAt: '2026-12-17T07:10:00.000Z', judgedAt: '2026-12-17T07:11:00.000Z', traceId: 'tr_12a31fd', sourceCode: 'struct Node { int value; Node* next; };', strippedCode: 'struct Node { int value; Node* next; };', publishedDeductions: null },
]

export const mockAppeals: AppealItem[] = [
  { id: 101, labId: 2, submissionId: 45, studentId: 'student-1', constraintId: 'C1', constraintDescription: 'The solution must use binary search.', deductionTenths: 40, reason: 'My loop halves the search range on every iteration.', status: 'pending', decisionReason: null, decidedBy: null, decidedAt: null, createdAt: '2026-12-10T11:15:00.000Z' },
]
