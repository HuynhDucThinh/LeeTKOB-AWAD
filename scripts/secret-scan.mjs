import { execSync } from 'node:child_process';

// Fail closed (AI-LOG A10): any git error must make this script exit non-zero.
// The previous version wrapped the history scan in `try { } catch { }`, so outside a
// git repository (or with a broken git) it printed "passed" and exited 0.

const SECRET_PATTERNS = [
  { name: 'OpenAI / API Secret Key', regex: /\bsk-[A-Za-z0-9]{20,}\b/ },
  { name: 'GitHub Personal Access Token', regex: /\bgh[pousr]_[A-Za-z0-9]{20,}\b/ },
  { name: 'Google AI / Cloud API Key', regex: /\bAIza[0-9A-Za-z\-_]{35}\b/ },
  { name: 'Private Key Block', regex: /-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----/ },
];

function git(args) {
  try {
    return execSync(`git ${args}`, {
      encoding: 'utf8',
      maxBuffer: 50 * 1024 * 1024,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
  } catch (error) {
    console.error(`SECURITY SCAN ERROR: 'git ${args}' failed: ${error.message.split('\n')[0]}`);
    process.exit(2);
  }
}

if (git('rev-parse --is-inside-work-tree').trim() !== 'true') {
  console.error('SECURITY SCAN ERROR: not inside a git work tree.');
  process.exit(2);
}

if (git('ls-files .env').trim()) {
  console.error('SECURITY FAILURE: .env is tracked by git!');
  process.exit(1);
}

if (git('rev-parse --is-shallow-repository').trim() === 'true') {
  console.error('SECURITY SCAN ERROR: shallow clone; fetch full history (fetch-depth: 0).');
  process.exit(2);
}

const commitCount = Number(git('rev-list --count --all').trim());
if (commitCount > 0) {
  const historyDiff = git('log --all -p');
  for (const pattern of SECRET_PATTERNS) {
    if (pattern.regex.test(historyDiff)) {
      console.error(`SECURITY FAILURE: Detected ${pattern.name} in git history!`);
      process.exit(1);
    }
  }
}

console.log(`Secret scan passed: 0 secrets found across ${commitCount} commits of git history.`);
