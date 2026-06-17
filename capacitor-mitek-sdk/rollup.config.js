import typescript from '@rollup/plugin-typescript';

export default {
  input: 'src/index.ts',
  output: [
    {
      file: 'dist/plugin.cjs.js',
      format: 'cjs',
      sourcemap: true,
      inlineDynamicImports: true,
    },
    {
      file: 'dist/plugin.js',
      format: 'iife',
      name: 'capacitorMitekSdk',
      globals: {
        '@capacitor/core': 'capacitorExports',
      },
      sourcemap: true,
      inlineDynamicImports: true,
    },
  ],
  plugins: [
    typescript({
      declaration: false,
      declarationDir: undefined,
      sourceMap: true,
    }),
  ],
  external: ['@capacitor/core'],
};
