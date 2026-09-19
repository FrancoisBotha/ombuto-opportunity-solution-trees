/*
 * Everything the OST screens need visually. Imported ONLY from components of the lazy OST chunk
 * (OstShell, TreesLanding) so Inter and the Vue Flow base CSS never load for the rest of the app.
 * Vue Flow's theme-default.css is deliberately not imported (it styles :root); OST nodes are custom.
 */
import '@fontsource/inter/400.css';
import '@fontsource/inter/500.css';
import '@fontsource/inter/600.css';
import '@vue-flow/core/dist/style.css';
import '@vue-flow/minimap/dist/style.css';
import './ost-tokens.css';
import './ost-base.css';
