import { registerPlugin } from '@capacitor/core';

import type { MitekSdkPlugin } from './definitions';

const MitekSdk = registerPlugin<MitekSdkPlugin>('MitekSdk', {
  web: () => import('./web').then(m => new m.MitekSdkWeb()),
});

export * from './definitions';
export { MitekSdk };
