import { stripTypeScriptTypes } from 'node:module';
import { fileURLToPath } from 'node:url';

import { compileScript, parse } from '@vue/compiler-sfc';
import { defineConfig } from 'vitest/config';

const appRoot = fileURLToPath(new URL('../src/main/webapp/', import.meta.url));

export default defineConfig({
  root: appRoot,
  esbuild: false,
  plugins: [
    {
      name: 'no-spawn-defines',
      enforce: 'pre',
      transform(code, id) {
        code = code.replaceAll('process.env.NODE_ENV', JSON.stringify('test'));
        if (!id.includes('/node_modules/')) {
          code = code
            .replaceAll('SERVER_API_URL', JSON.stringify('/'))
            .replaceAll('APP_VERSION', JSON.stringify('DEV'))
            .replaceAll('I18N_HASH', JSON.stringify('generated_hash'));
        }
        return { code, map: null };
      },
    },
    {
      name: 'native-vue-transform',
      enforce: 'pre',
      transform(code, id) {
        if (!id.endsWith('.vue')) return null;
        const { descriptor, errors } = parse(code, { filename: id });
        if (errors.length) throw errors[0];
        const compiled = compileScript(descriptor, { id: 'rtc004', inlineTemplate: true });
        return { code: stripTypeScriptTypes(compiled.content, { mode: 'transform', sourceMap: true }), map: null };
      },
    },
    {
      name: 'native-typescript-transform',
      transform(code, id) {
        const path = id.split('?', 1)[0];
        if (id.includes('/node_modules/') || (!path.endsWith('.ts') && !path.endsWith('.vue'))) return null;
        return { code: stripTypeScriptTypes(code, { mode: 'transform', sourceMap: true }), map: null };
      },
    },
  ],
  resolve: {
    alias: {
      vue: 'vue/dist/vue.esm-bundler.js',
      '@': fileURLToPath(new URL('../src/main/webapp/app/', import.meta.url)),
      '@content': fileURLToPath(new URL('../src/main/webapp/content/', import.meta.url)),
    },
  },
  test: {
    globals: true,
    environment: 'happy-dom',
    setupFiles: [fileURLToPath(new URL('../src/main/webapp/app/test-setup.ts', import.meta.url))],
    pool: 'threads',
    reporters: ['default'],
  },
});
