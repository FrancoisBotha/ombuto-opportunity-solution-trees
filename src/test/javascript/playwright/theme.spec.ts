import { type Page, expect, test } from '@playwright/test';

/**
 * THEME-1: the light / dark theme foundation and the app chrome.
 *
 * Rules under test:
 *   - dark is the default on a first visit; the OS preference is never consulted;
 *   - the navbar toggle flips `data-theme` on <html> and the choice survives reload and navigation;
 *   - navbar and sidebar take their colours from content/css/theme.css in BOTH themes;
 *   - the sidebar RAIL is deliberately Nocturne dark in both themes (a product decision — see the
 *     theming section of docs/Architecture/Architecture.md), while the page content follows;
 *   - the OST tree canvas is an artboard: byte-identical computed colours in both themes;
 *   - ordinary pages stay readable in light AND dark (no white-on-white, no black-on-black).
 *
 * Reads only the seeded "Team Jupiter" tree and never changes it; creates no data, so there is
 * nothing to register with support/cleanup.ts.
 */

const STORAGE_KEY = 'ombuto-theme';

interface MyTeam {
  id: number;
  name: string;
}

/**
 * Custom-property values on <html> for the current theme, each normalised to the `rgb(...)` form a
 * computed `background-color` reports (a token's raw value is whatever the author typed).
 */
async function tokens(page: Page, names: string[]): Promise<Record<string, string>> {
  return page.evaluate(props => {
    const probe = document.createElement('span');
    probe.style.display = 'none';
    document.documentElement.append(probe);
    try {
      return Object.fromEntries(
        props.map(p => {
          probe.style.color = '';
          probe.style.color = `var(${p})`;
          return [p, getComputedStyle(probe).color];
        }),
      );
    } finally {
      probe.remove();
    }
  }, names);
}

async function styles(page: Page, selector: string, props: string[]): Promise<Record<string, string> | null> {
  return page.evaluate(
    ([sel, wanted]) => {
      const el = document.querySelector(sel as string);
      if (!el) return null;
      const style = getComputedStyle(el);
      return Object.fromEntries((wanted as string[]).map(p => [p, style.getPropertyValue(p)]));
    },
    [selector, props] as const,
  );
}

const channels = (colour: string): [number, number, number] => {
  const [r, g, b] = colour.match(/[\d.]+/g)!.map(Number);
  return [r, g, b];
};

const luminance = (colour: string): number => {
  const [r, g, b] = channels(colour).map(v => {
    const c = v / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};

const contrast = (a: string, b: string): number => {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
};

/** The theme currently on <html>, plus Bootstrap's mirror of it. */
const themeAttributes = (page: Page) =>
  page.evaluate(() => ({
    theme: document.documentElement.getAttribute('data-theme'),
    bootstrap: document.documentElement.getAttribute('data-bs-theme'),
  }));

/**
 * The chrome transitions its colours, and getComputedStyle reports the interpolated value while a
 * transition runs — so every colour assertion waits for them to finish first.
 */
async function waitForColoursToSettle(page: Page) {
  await expect
    .poll(() => page.evaluate(() => document.getAnimations().filter(a => a.playState === 'running').length), {
      intervals: [50, 50, 100, 200],
      timeout: 5_000,
    })
    .toBe(0);
}

async function setTheme(page: Page, wanted: 'dark' | 'light') {
  const toggle = page.getByTestId('themeToggle');
  await expect(toggle).toBeVisible();
  if ((await themeAttributes(page)).theme !== wanted) {
    await toggle.click();
  }
  await expect.poll(async () => (await themeAttributes(page)).theme).toBe(wanted);
  await waitForColoursToSettle(page);
}

/** Starts every test from a browser that has never chosen a theme. */
async function forgetTheme(page: Page) {
  await page.goto('/');
  await page.evaluate(key => {
    try {
      localStorage.removeItem(key);
    } catch {
      /* ignore */
    }
  }, STORAGE_KEY);
  await page.reload();
}

/** Fails the test on any console error or uncaught page error. */
function watchForErrors(page: Page): string[] {
  const problems: string[] = [];
  page.on('console', message => {
    if (message.type() === 'error') problems.push(`console: ${message.text()}`);
  });
  page.on('pageerror', error => problems.push(`pageerror: ${error.message}`));
  return problems;
}

test.describe('theme', () => {
  test.setTimeout(90_000);

  test('a first visit is dark, and says so on <html>', async ({ page }) => {
    const problems = watchForErrors(page);
    await forgetTheme(page);

    expect(await themeAttributes(page)).toEqual({ theme: 'dark', bootstrap: 'dark' });
    expect(await page.evaluate(key => localStorage.getItem(key), STORAGE_KEY)).toBeNull();

    // The chrome is on the Nocturne ground, not the old green.
    const navbar = (await styles(page, '.va-navbar', ['background-color']))!;
    expect(navbar['background-color']).toBe('rgb(22, 24, 38)');
    expect(problems).toEqual([]);
  });

  test('the OS preference is ignored: a light-preferring browser still opens dark', async ({ browser }) => {
    const context = await browser.newContext({
      colorScheme: 'light',
      storageState: 'target/playwright/.auth/user.json',
    });
    try {
      const page = await context.newPage();
      await page.goto('/');
      await page.evaluate(key => localStorage.removeItem(key), STORAGE_KEY);
      await page.reload();
      expect((await themeAttributes(page)).theme).toBe('dark');
    } finally {
      await context.close();
    }
  });

  test('the toggle switches the theme and remembers it across reload and navigation', async ({ page }) => {
    const problems = watchForErrors(page);
    await forgetTheme(page);

    const toggle = page.getByTestId('themeToggle');
    await expect(toggle).toHaveAttribute('aria-label', 'Switch to light theme');

    await toggle.click();
    expect(await themeAttributes(page)).toEqual({ theme: 'light', bootstrap: 'light' });
    await expect(toggle).toHaveAttribute('aria-label', 'Switch to dark theme');
    expect(await page.evaluate(key => localStorage.getItem(key), STORAGE_KEY)).toBe('light');

    // Survives a full reload (the inline pre-paint script in index.html).
    await page.reload();
    expect((await themeAttributes(page)).theme).toBe('light');

    // Survives client-side navigation.
    await page.getByTestId('myTeamsMenu').locator('a').click();
    await expect(page).toHaveURL(/\/teams$/);
    expect((await themeAttributes(page)).theme).toBe('light');

    // And a hard navigation to another page.
    await page.goto('/opportunity');
    expect((await themeAttributes(page)).theme).toBe('light');

    await page.getByTestId('themeToggle').click();
    expect((await themeAttributes(page)).theme).toBe('dark');
    expect(await page.evaluate(key => localStorage.getItem(key), STORAGE_KEY)).toBe('dark');
    expect(problems).toEqual([]);
  });

  test('navbar and sidebar are painted from the tokens in both themes', async ({ page }) => {
    await forgetTheme(page);

    // The sidebar only mounts once the account call has come back.
    await expect(page.getByTestId('sidebar')).toBeVisible();

    for (const theme of ['dark', 'light'] as const) {
      await setTheme(page, theme);
      const t = await tokens(page, ['--ost-navbar-bg', '--ost-sidebar-bg', '--ost-text', '--ost-sidebar-fg', '--ost-bg']);

      const navbar = (await styles(page, '.va-navbar', ['background-color']))!;
      const sidebar = (await styles(page, '[data-cy="sidebar"]', ['background-color']))!;
      const container = (await styles(page, '.va-container', ['background-color']))!;

      // The token must resolve to a real colour, and the element must be wearing it.
      for (const [name, value] of Object.entries(t)) {
        expect(value, `${name} in ${theme}`).not.toBe('');
      }
      expect(navbar['background-color'], `navbar in ${theme}`).toBe(t['--ost-navbar-bg']);
      expect(sidebar['background-color'], `sidebar in ${theme}`).toBe(t['--ost-sidebar-bg']);
      expect(container['background-color'], `.va-container in ${theme}`).toBe(t['--ost-bg']);

      // The navbar and the page content DO follow the theme (the rail does not — next test).
      expect(navbar['background-color'], `navbar must differ per theme (${theme})`).toBe(
        theme === 'dark' ? 'rgb(22, 24, 38)' : 'rgb(255, 255, 255)',
      );
      expect(container['background-color'], `content must differ per theme (${theme})`).toBe(
        theme === 'dark' ? 'rgb(22, 24, 38)' : 'rgb(243, 245, 254)',
      );

      // Chrome text is legible on the chrome ground (AA body text).
      const title = (await styles(page, '.va-navbar .navbar-title', ['color']))!;
      expect(contrast(title.color, navbar['background-color']), `navbar title in ${theme}`).toBeGreaterThanOrEqual(4.5);

      const menuItem = (await styles(page, '[data-cy="sidebar"] .menu-item', ['color']))!;
      expect(
        contrast(menuItem.color, sidebar['background-color']),
        `sidebar row in ${theme}: ${menuItem.color} on ${sidebar['background-color']}`,
      ).toBeGreaterThanOrEqual(4.5);

      // The NAVIGATION section label and the accent-tinted active row exist in both themes.
      await expect(page.locator('[data-cy="sidebar"] .menu-section-label')).toHaveText(/navigation/i);
    }

    // The old green chrome is gone for good.
    await setTheme(page, 'dark');
    const navbar = (await styles(page, '.va-navbar', ['background-color']))!;
    const sidebar = (await styles(page, '[data-cy="sidebar"]', ['background-color']))!;
    expect([navbar['background-color'], sidebar['background-color']]).not.toContain('rgb(44, 53, 49)');
    expect([navbar['background-color'], sidebar['background-color']]).not.toContain('rgb(38, 48, 41)');
  });

  /**
   * A product decision, not an oversight: the rail is the app's spine and shares the tree canvas's
   * artboard ground, so navigation reads the same wherever you are. If this test starts failing
   * because someone added `--ost-sidebar-*` overrides to `:root[data-theme='light']`, the fix is to
   * remove those overrides, not to relax this test. See docs/Architecture/Architecture.md §7.
   */
  test('the sidebar rail stays Nocturne dark in both themes', async ({ page }) => {
    await forgetTheme(page);
    await expect(page.getByTestId('sidebar')).toBeVisible();

    const GROUND = 'rgb(22, 24, 38)'; // --ost-nocturne-ground, #161826

    const seen: Record<string, Record<string, string>> = {};
    for (const theme of ['dark', 'light'] as const) {
      await setTheme(page, theme);
      const sidebar = (await styles(page, '[data-cy="sidebar"]', ['background-color']))!;
      expect(sidebar['background-color'], `the rail in ${theme}`).toBe(GROUND);

      // Its text is the dark-rail text, and legible on that ground, in either theme.
      const row = (await styles(page, '[data-cy="sidebar"] .menu-item', ['color']))!;
      const label = (await styles(page, '[data-cy="sidebar"] .menu-section-label', ['color']))!;
      expect(contrast(row.color, GROUND), `rail row in ${theme}`).toBeGreaterThanOrEqual(4.5);
      expect(contrast(label.color, GROUND), `rail NAVIGATION label in ${theme}`).toBeGreaterThanOrEqual(4.5);
      seen[theme] = { rail: sidebar['background-color'], row: row.color, label: label.color };
    }

    // Not merely "dark enough" in each theme — the very same colours.
    expect(seen.light).toEqual(seen.dark);
  });

  test('the dev-profile marker is a navbar pill, not a banner over the sidebar', async ({ page }) => {
    await forgetTheme(page);
    const marker = page.getByTestId('ribbon');
    await expect(marker, 'the dev profile must be marked').toBeVisible();
    await expect(marker).toHaveText(/dev/i);

    const navbar = page.locator('.va-navbar');
    const sidebar = page.getByTestId('sidebar');
    await expect(sidebar).toBeVisible();

    // It lives inside the navbar…
    expect(await marker.evaluate(el => !!el.closest('.va-navbar'))).toBe(true);
    // …and cannot overlap the rail: it ends above the sidebar and right of it.
    const [pill, bar, rail] = await Promise.all([marker.boundingBox(), navbar.boundingBox(), sidebar.boundingBox()]);
    expect(pill!.y + pill!.height).toBeLessThanOrEqual(bar!.y + bar!.height + 1);
    expect(pill!.y + pill!.height).toBeLessThanOrEqual(rail!.y + 1);

    // Readable in both themes, and never the same as the neutral version badge beside it.
    for (const theme of ['dark', 'light'] as const) {
      await setTheme(page, theme);
      const seen = await marker.evaluate(el => {
        const style = getComputedStyle(el);
        const bar2 = getComputedStyle(el.closest('.va-navbar')!);
        return { color: style.color, background: bar2.backgroundColor, border: style.borderTopColor };
      });
      expect(contrast(seen.color, seen.background), `dev pill in ${theme}: ${seen.color} on ${seen.background}`).toBeGreaterThanOrEqual(
        4.5,
      );
      const version = (await styles(page, '.va-navbar .navbar-version', ['color']))!;
      expect(seen.color, `dev pill must stand out in ${theme}`).not.toBe(version.color);
    }

    // The old diagonal banner is gone.
    expect(await page.locator('.ribbon').evaluate(el => getComputedStyle(el).transform)).toBe('none');
    expect(await page.locator('.ribbon').evaluate(el => getComputedStyle(el).position)).not.toBe('absolute');
  });

  test('the active sidebar row is accent-tinted', async ({ page }) => {
    await forgetTheme(page);
    await page.goto('/teams');
    const active = page.locator('[data-cy="sidebar"] li.active').first();
    await expect(active).toBeVisible();

    for (const theme of ['dark', 'light'] as const) {
      await setTheme(page, theme);
      const seen = await active.evaluate(el => {
        const style = getComputedStyle(el);
        return { background: style.backgroundColor, ring: style.boxShadow };
      });
      const sidebar = (await styles(page, '[data-cy="sidebar"]', ['background-color']))!;
      expect(seen.background, `active row tint in ${theme}`).not.toBe(sidebar['background-color']);
      expect(seen.ring, `active row ring in ${theme}`).not.toBe('none');
    }
  });

  test('the OST canvas is identical in both themes', async ({ page }) => {
    const problems = watchForErrors(page);
    await forgetTheme(page);

    const teams = (await (await page.request.get('/api/team-management/my-teams')).json()) as MyTeam[];
    const jupiter = teams.find(team => team.name === 'Team Jupiter');
    expect(jupiter, 'seeded Team Jupiter').toBeTruthy();

    const capture = async () => {
      await expect(page.getByTestId('ost-canvas')).toBeVisible();
      await expect(page.locator('.ost-node').first()).toBeVisible();
      return page.evaluate(() => {
        const pick = (selector: string, props: string[]) => {
          const el = document.querySelector(selector);
          if (!el) return null;
          const style = getComputedStyle(el);
          return Object.fromEntries(props.map(p => [p, style.getPropertyValue(p)]));
        };
        const box = ['background-color', 'color', 'border-top-color', 'border-top-width', 'box-shadow'];
        return {
          root: pick('.ost-root', ['background-color', 'color', 'font-family']),
          canvas: pick('[data-cy="ost-canvas"]', ['background-color', 'background-image', 'color']),
          toolbar: pick('.ost-toolbar', box),
          searchInput: pick('.ost-toolbar__search-input', box),
          chip: pick('.ost-toolbar__chip', box),
          node: pick('.ost-node', box),
          nodeTitle: pick('.ost-node__title', ['color']),
          nodeKicker: pick('.ost-node__kicker', ['color']),
          badge: pick('.ost-badge', ['background-color', 'color', 'border-top-color']),
          nav: pick('.ost-nav', ['background-color', 'color', 'border-bottom-color']),
        };
      });
    };

    await page.goto(`/trees/${jupiter!.id}/canvas`);
    await setTheme(page, 'dark');
    const dark = await capture();

    await setTheme(page, 'light');
    const light = await capture();

    expect(light).toEqual(dark);
    // …and it really is the Nocturne artboard, not a light surface that happens to match.
    expect(dark.root!['background-color']).toBe('rgb(22, 24, 38)');
    expect(dark.nodeTitle!.color).toBe('rgb(233, 233, 237)');
    expect(luminance(dark.node!['background-color']), 'node surface stays dark').toBeLessThan(0.05);
    expect(problems).toEqual([]);
  });

  test('/teams and an admin entity list read correctly in both themes', async ({ page }) => {
    const problems = watchForErrors(page);
    await forgetTheme(page);

    for (const path of ['/teams', '/opportunity']) {
      for (const theme of ['dark', 'light'] as const) {
        await page.goto(path);
        await setTheme(page, theme);
        await expect(page.locator('#page-heading')).toBeVisible();

        const readable = await page.evaluate(() => {
          const luminanceOf = (colour: string) => {
            const [r, g, b] = colour.match(/[\d.]+/g)!.map(Number);
            return [r, g, b]
              .map(v => {
                const c = v / 255;
                return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
              })
              .reduce((sum, v, i) => sum + [0.2126, 0.7152, 0.0722][i] * v, 0);
          };
          const backgroundOf = (start: Element) => {
            let el: Element | null = start;
            while (el) {
              const bg = getComputedStyle(el).backgroundColor;
              const alpha = Number(bg.match(/[\d.]+/g)?.[3] ?? 1);
              if (alpha > 0.9 && bg !== 'transparent') return bg;
              el = el.parentElement;
            }
            return getComputedStyle(document.body).backgroundColor;
          };
          const ratio = (fg: string, bg: string) => {
            const [hi, lo] = [luminanceOf(fg), luminanceOf(bg)].sort((a, b) => b - a);
            return (hi + 0.05) / (lo + 0.05);
          };
          const samples = ['#page-heading', '.jh-card', 'body', 'table th', 'table td', '.btn-primary', '.footer'];
          return samples
            .map(selector => {
              const el = document.querySelector(selector);
              if (!el) return null;
              const style = getComputedStyle(el);
              return { selector, ratio: ratio(style.color, backgroundOf(el)) };
            })
            .filter((s): s is { selector: string; ratio: number } => s !== null);
        });

        expect(readable.length, `${path} in ${theme}`).toBeGreaterThan(2);
        for (const sample of readable) {
          expect(sample.ratio, `${sample.selector} on ${path} in ${theme}`).toBeGreaterThanOrEqual(4.5);
        }
      }
    }
    expect(problems).toEqual([]);
  });
});
