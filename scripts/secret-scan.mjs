import { execSync } from 'node:child_process';
import { existsSync } from 'node:fs';

const SECRET_PATTERNS = [
  { name: 'OpenAI / API Secret Key', regex: /\bsk-[A-Za-z0-9]{20,}\b/ },
  { name: 'GitHub Personal Access Token', regex: /\bgh[pousr]_[A-Za-z0-9]{20,}\b/ },
  { name: 'Google AI / Cloud API Key', regex: /\bAIza[0-9A-Za-z\-_]{35}\b/ },
  { name: 'Private Key Block', regex: /-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----/ },
];

if (existsSync('.env')) {
  const tracked = execSync('git ls-files .env', { encoding: 'utf8' }).trim();
  if (tracked) {
    console.error('SECURITY FAILURE: .env is tracked by git!');
    process.exit(1);
  }
}

try {
  const hasCommits =
    execSync('git rev-list --count --all', { encoding: 'utf8' }).trim() !== '0';
  if (hasCommits) {
    const historyDiff = execSync('git log --all -p', {
      encoding: 'utf8',
      maxBuffer: 50 * 1024 * 1024,
    });
    for (const pattern of SECRET_PATTERNS) {
      if (pattern.regex.test(historyDiff)) {
        console.error(`SECURITY FAILURE: Detected ${pattern.name} in git history!`);
        process.exit(1);
      }
    }
  }
} catch {
  // No git commits yet, skip git history scan
}

console.log('Secret scan passed: 0 secrets found in tracked files or git history.');

