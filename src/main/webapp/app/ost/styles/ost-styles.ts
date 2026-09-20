/*
 * Everything the OST screens need visually. Imported ONLY from components of the lazy OST chunk
 * (OstShell, TreesLanding) so Inter and the Vue Flow base CSS never load for the rest of the app.
 * Vue Flow's theme-default.css is deliberately not imported (it styles :root); OST nodes are custom.
 */
import '@fontsource/inter/400.css';
import '@fontsource/inter/500.css';
import '@fontsource/inter/600.css';
import '@vue-flow/core/dist/style.css';
// The palette itself. Already in the entry bundle (main.ts) — imported again so the OST chunk can
// never render without the hex it reads through `--ost-nocturne-*` and the ramps.
import '@content/css/theme.css';
import './ost-tokens.css';
import './ost-base.css';
