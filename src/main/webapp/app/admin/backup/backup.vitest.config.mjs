import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { compileTemplate, parse } from '@vue/compiler-sfc';
import ts from 'typescript';
import { defineConfig } from 'vitest/config';

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../../../../..');
const webappRoot = path.join(repositoryRoot, 'src/main/webapp');

function transpileTypeScript() {
  return {
    name: 'backup-test-typescript',
    enforce: 'pre',
    transform(code, id) {
      const cleanId = id.split('?')[0];
      if (!/\.[cm]?tsx?$/.test(cleanId)) return null;
      return {
        code: ts.transpileModule(code, {
          compilerOptions: {
            module: ts.ModuleKind.ESNext,
            target: ts.ScriptTarget.ES2022,
            sourceMap: true,
          },
          fileName: cleanId,
        }).outputText,
        map: null,
      };
    },
  };
}

function compileVueComponent() {
  return {
    name: 'backup-test-vue',
    enforce: 'pre',
    transform(source, id) {
      if (!id.endsWith('.vue')) return null;

      const { descriptor, errors } = parse(source, { filename: id });
      if (errors.length) throw errors[0];
      if (!descriptor.script?.src || !descriptor.template) throw new Error(`Unsupported test component: ${id}`);

      const template = compileTemplate({
        source: descriptor.template.content,
        filename: id,
        id: `data-v-${Buffer.from(id).toString('hex').slice(-8)}`,
      });
      if (template.errors.length) throw template.errors[0];

      return {
        code: `import component from ${JSON.stringify(descriptor.script.src)};\n${template.code}\ncomponent.render = render;\nexport default component;`,
        map: null,
      };
    },
  };
}

export default defineConfig({
  root: webappRoot,
  esbuild: false,
  plugins: [compileVueComponent(), transpileTypeScript()],
  resolve: {
    alias: {
      '@': path.join(webappRoot, 'app'),
      '@content': path.join(webappRoot, 'content'),
      vue: 'vue',
    },
  },
  test: {
    globals: true,
    environment: 'happy-dom',
    setupFiles: [path.join(webappRoot, 'app/test-setup.ts')],
    pool: 'threads',
    reporters: ['default'],
  },
});
