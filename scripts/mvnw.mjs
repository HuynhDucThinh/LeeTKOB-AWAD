import { spawnSync } from 'node:child_process';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const backendDir = resolve(__dirname, '..', 'backend');
const isWin = process.platform === 'win32';
const cmd = isWin ? 'mvnw.cmd' : './mvnw';
const args = process.argv.slice(2);

const result = spawnSync(cmd, args, {
  cwd: backendDir,
  stdio: 'inherit',
  shell: isWin,
});

if (result.error) {
  console.error('Failed to execute Maven Wrapper:', result.error.message);
  process.exit(1);
}

process.exit(result.status ?? 1);

