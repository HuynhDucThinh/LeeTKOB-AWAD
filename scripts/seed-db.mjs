import { spawnSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const sqlPath = resolve(
  __dirname,
  '..',
  'backend',
  'src',
  'main',
  'resources',
  'db',
  'seed',
  'seed_sample_data.sql'
);

const container = process.env.POSTGRES_CONTAINER || 'leetkob-postgres';
const dbUser = process.env.DB_USERNAME || 'postgres';
const dbName = process.env.POSTGRES_DB || 'leetkob';
const sql = readFileSync(sqlPath, 'utf8');

const result = spawnSync(
  'docker',
  ['exec', '-i', container, 'psql', '-U', dbUser, '-d', dbName, '-v', 'ON_ERROR_STOP=1'],
  {
    input: sql,
    stdio: ['pipe', 'inherit', 'inherit'],
  }
);

if (result.error) {
  console.error('Failed to run docker exec:', result.error.message);
  process.exit(1);
}

if (result.status !== 0) {
  console.error(
    `Seed failed (exit ${result.status}). Ensure Docker container "${container}" is running and Flyway migrations have been applied.`
  );
  process.exit(result.status ?? 1);
}

console.log(`Sample data seeded successfully into ${container}/${dbName}.`);
