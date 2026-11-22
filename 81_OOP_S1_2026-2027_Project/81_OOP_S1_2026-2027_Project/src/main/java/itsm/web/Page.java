package itsm.web;

/** The single-page web front end (HTML, CSS and JavaScript), served by DashboardServer. */
final class Page {

    private Page() {
    }

    /** Joined at start-up: one string constant cannot be longer than 64 KB in Java. */
    static final String HTML = String.join("", Parts.HEAD, Parts.BODY, Parts.SCRIPT1, Parts.SCRIPT2);

    private static final class Parts {
        static final String HEAD = """
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>I-ITSM | Campus Service Desk</title>
    <link rel="icon" href="data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E%3Cpath d='M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z' fill='%23131416'/%3E%3C/svg%3E">
    <script>
        try {
            const savedTheme = localStorage.getItem('itsm-theme');
            if (savedTheme === 'dark' || savedTheme === 'light') {
                document.documentElement.dataset.theme = savedTheme;
            }
        } catch (e) {
            // Storage blocked: follow the system setting.
        }
    </script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Literata:opsz,wght@7..72,400;7..72,500;7..72,600;7..72,700&display=swap" rel="stylesheet">
    <style>
        :root {
            --brand: #131416;
            --brand-dark: #2a2c31;
            --brand-soft: #ecedef;
            --brand-line: #c9cbd0;
            --bar: #0d0e10;
            --nav: #0d0e10;
            --nav-text: #c9cbd0;
            --text: #131416;
            --muted: #5d6067;
            --line: #d6d8dc;
            --page: #e8e9eb;
            --card: #ffffff;
            --red: #af3934;
            --red-soft: #f6e3e2;
            --orange: #b4581f;
            --orange-soft: #f6e8dc;
            --amber: #a4741a;
            --amber-soft: #f5eddc;
            --green: #3a8460;
            --green-soft: #e4f1ea;
            --blue: #3b3e44;
            --blue-soft: #e4e5e8;
            --purple: #4a4d55;
            --purple-soft: #ecedef;
            --gray-soft: #eeeff1;
            --radius: 8px;
            --shadow: 0 1px 2px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.08);
            --surface: #ffffff;
            --on-brand: #ffffff;
            --field-line: #c9cbd0;
            --soft: #f4f5f6;
            --rule: #ecedef;
            --label: #3b3e44;
            --faint: #9a9da4;
            --track: #e3e4e7;
            --note-bg: #f7f7f5;
            --note-line: #d8d8d1;
            --fix-bg: #eef5f0;
            --fix-line: #cfe2d6;
            --red-line: #e3b3b0;
            color-scheme: light;
        }
        /* Dark theme: follows the system until the switch is used, then the saved choice wins. */
        @media (prefers-color-scheme: dark) {
            :root:not([data-theme="light"]) {
                --brand: #e4e5e8;
                --brand-dark: #ffffff;
                --brand-soft: #232529;
                --brand-line: #3a3d42;
                --bar: #000000;
                --nav: #000000;
                --text: #ecedef;
                --muted: #9a9da4;
                --line: #2a2c31;
                --page: #0a0b0c;
                --card: #141518;
                --red: #e36a62;
                --red-soft: #2e1a19;
                --orange: #e08a52;
                --orange-soft: #2e2016;
                --amber: #d8a445;
                --amber-soft: #2b2414;
                --green: #5db98a;
                --green-soft: #172a20;
                --blue: #c9cbd0;
                --blue-soft: #26282d;
                --purple: #b4b7bd;
                --purple-soft: #232529;
                --gray-soft: #1f2125;
                --shadow: none;
                --surface: #141518;
                --on-brand: #0d0e10;
                --field-line: #3a3d42;
                --soft: #1b1c20;
                --rule: #222428;
                --label: #c9cbd0;
                --faint: #6f727a;
                --track: #26282d;
                --note-bg: #1c1c19;
                --note-line: #3a3a33;
                --fix-bg: #16231b;
                --fix-line: #2c4a37;
                --red-line: #5a2b28;
                color-scheme: dark;
            }
        }
        :root[data-theme="dark"] {
            --brand: #e4e5e8;
            --brand-dark: #ffffff;
            --brand-soft: #232529;
            --brand-line: #3a3d42;
            --bar: #000000;
            --nav: #000000;
            --text: #ecedef;
            --muted: #9a9da4;
            --line: #2a2c31;
            --page: #0a0b0c;
            --card: #141518;
            --red: #e36a62;
            --red-soft: #2e1a19;
            --orange: #e08a52;
            --orange-soft: #2e2016;
            --amber: #d8a445;
            --amber-soft: #2b2414;
            --green: #5db98a;
            --green-soft: #172a20;
            --blue: #c9cbd0;
            --blue-soft: #26282d;
            --purple: #b4b7bd;
            --purple-soft: #232529;
            --gray-soft: #1f2125;
            --shadow: none;
            --surface: #141518;
            --on-brand: #0d0e10;
            --field-line: #3a3d42;
            --soft: #1b1c20;
            --rule: #222428;
            --label: #c9cbd0;
            --faint: #6f727a;
            --track: #26282d;
            --note-bg: #1c1c19;
            --note-line: #3a3a33;
            --fix-bg: #16231b;
            --fix-line: #2c4a37;
            --red-line: #5a2b28;
            color-scheme: dark;
        }
        .theme-toggle .sun { display: none; }
        [data-dark="true"] .theme-toggle .sun { display: inline; }
        [data-dark="true"] .theme-toggle .moon { display: none; }
        .auth-art .theme-switch { display: inline-flex; align-items: center; gap: 10px; padding: 4px 0; font-size: 0.92rem; font-weight: 400; color: #8d9097; background: none; border: 0; }
        .auth-art .theme-switch:hover { color: #e7e8ea; background: none; }
        .switch { position: relative; width: 34px; height: 20px; background: #1f2125; border: 1px solid #8d9097; border-radius: 999px; }
        .switch::after { content: ""; position: absolute; top: 2px; left: 2px; width: 14px; height: 14px; background: #e7e8ea; border-radius: 50%; transition: transform 0.15s; }
        .theme-switch[aria-pressed="true"] .switch { background: #e7e8ea; border-color: #e7e8ea; }
        .theme-switch[aria-pressed="true"] .switch::after { background: #0d0e10; transform: translateX(14px); }
        .auth-foot { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px 24px; }
        * { box-sizing: border-box; }
        html { font-size: 14px; }
        body { margin: 0; font-family: "Literata", Georgia, "Times New Roman", serif; line-height: 1.55; color: var(--text); background: var(--page); }
        a { color: var(--brand); text-decoration: none; }
        a:hover { text-decoration: underline; }
        :focus-visible { outline: 2px solid var(--brand); outline-offset: 2px; }
        .hidden { display: none !important; }
        svg.i { width: 18px; height: 18px; flex: none; fill: none; stroke: currentColor; stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }

        /* ---------- buttons and form controls ---------- */
        button, .btn { display: inline-flex; align-items: center; justify-content: center; gap: 6px; margin: 0; padding: 7px 14px; font: inherit; font-weight: 600; color: var(--on-brand); background: var(--brand); border: 1px solid var(--brand); border-radius: 6px; cursor: pointer; white-space: nowrap; }
        button:hover { background: var(--brand-dark); }
        button.ghost { color: var(--text); background: var(--surface); border-color: var(--field-line); }
        button.ghost:hover { background: var(--gray-soft); }
        button.warn { color: var(--red); background: var(--surface); border-color: var(--red-line); }
        button.warn:hover { background: var(--red-soft); }
        button.locked { color: var(--faint); background: var(--gray-soft); border-color: var(--line); cursor: not-allowed; }
        button.link { padding: 4px 2px; color: var(--brand); background: none; border-color: transparent; font-weight: 600; }
        button.small { padding: 4px 10px; font-size: 0.92rem; }
        button[disabled] { opacity: 0.6; cursor: wait; }
        label { display: block; margin: 14px 0 5px; font-weight: 600; color: var(--label); }
        .help { display: block; margin-top: 1px; font-weight: 400; color: var(--muted); font-size: 0.92rem; }
        input, select, textarea { width: 100%; padding: 8px 10px; font: inherit; color: var(--text); background: var(--surface); border: 1px solid var(--field-line); border-radius: 6px; }
        input:focus, select:focus, textarea:focus { border-color: var(--brand); outline: 3px solid var(--brand-soft); }
        textarea { min-height: 96px; resize: vertical; }
        .row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px; }
        .checks { display: flex; flex-wrap: wrap; gap: 6px 18px; }
        .checks label { display: flex; align-items: center; gap: 6px; margin: 0; font-weight: 400; }
        .checks input { width: auto; }

        /* ---------- sign-in screen ---------- */
        .auth { display: grid; grid-template-columns: 41% 1fr; min-height: 100vh; background: var(--page); }
        .auth-art { display: flex; flex-direction: column; justify-content: space-between; gap: 32px; padding: 52px 64px 44px; color: #e7e8ea; background: #0d0e10; }
        .auth-art .mark { display: flex; align-items: center; gap: 12px; }
        .auth-art .logo-box { display: grid; place-items: center; width: 40px; height: 40px; color: #e7e8ea; border: 1.5px solid #8d9097; border-radius: 8px; }
        .auth-art .mark b { display: block; font-size: 1.2rem; color: #ffffff; }
        .auth-art .mark small { display: block; font-size: 0.86rem; color: #8d9097; }
        .auth-art h2 { max-width: 14ch; margin: 0; font-size: clamp(2.1rem, 3.1vw, 3rem); font-weight: 700; line-height: 1.14; letter-spacing: -0.012em; color: #ffffff; }
        .auth-art .org { font-size: 0.92rem; color: #8d9097; }
        .auth-form { display: flex; align-items: center; justify-content: center; padding: 48px 24px; }
        .auth-box { width: 100%; max-width: 480px; }
        .pill { display: inline-block; padding: 2px 9px; font-size: 0.8rem; font-weight: 600; color: var(--muted); background: var(--surface); border: 1px solid var(--line); border-radius: 4px; }
        .auth-box h1 { margin: 16px 0 2px; font-size: 2.1rem; font-weight: 700; letter-spacing: -0.012em; }
        .auth-box .sub { margin: 0 0 26px; font-size: 1.05rem; color: var(--muted); }
        .caps { display: block; margin-bottom: 10px; font-size: 0.78rem; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; color: var(--muted); }
        .seg { display: flex; flex-direction: column; gap: 10px; margin-bottom: 8px; }
        .seg button { display: grid; grid-template-columns: 16px minmax(0, 1fr) auto; align-items: center; gap: 16px; padding: 14px 18px; font-weight: 400; text-align: left; white-space: normal; color: var(--text); background: var(--surface); border: 1px solid var(--line); border-radius: 6px; }
        .seg button:hover { background: var(--surface); border-color: var(--faint); }
        .seg .radio { width: 16px; height: 16px; border: 1.5px solid var(--faint); border-radius: 50%; }
        .seg button.on { border-color: var(--text); box-shadow: 0 0 0 1px var(--text); }
        .seg button.on .radio { border: 5px solid var(--text); }
        .seg b { display: block; font-size: 1.05rem; }
        .seg small { display: block; color: var(--muted); }
        .seg .acct { font-size: 0.86rem; text-align: right; color: var(--muted); }
        .seg .acct strong { display: block; font-size: 0.95rem; color: var(--text); }
        .auth-box .wide { width: 100%; margin-top: 22px; padding: 12px; font-size: 1.05rem; }
        .auth-alt { margin-top: 14px; text-align: center; color: var(--muted); }
        .fine { margin: 12px 0 0; font-size: 0.92rem; color: var(--muted); }
        .error { margin-top: 12px; padding: 8px 10px; color: var(--red); background: var(--red-soft); border-radius: 6px; }
        .error:empty { display: none; }
        @media (max-width: 860px) { .auth { grid-template-columns: 1fr; } .auth-art { padding: 28px 20px; } .auth-art h2 { font-size: 1.9rem; } .auth-form { padding: 28px 16px 40px; } }

        /* ---------- application frame ---------- */
        .topbar { position: sticky; top: 0; z-index: 10; display: flex; align-items: center; gap: 16px; height: 54px; padding: 0 18px; color: #ffffff; background: var(--bar); }
        .topbar .logo { display: flex; align-items: center; gap: 8px; min-width: 196px; font-size: 1.15rem; font-weight: 700; color: #ffffff; }
        .topbar .logo small { font-size: 0.85rem; font-weight: 400; color: #8d9097; }
        .topbar .menu { display: none; padding: 6px; color: #ffffff; background: transparent; border: 0; }
        .search { position: relative; flex: 1; max-width: 520px; }
        .search svg.i { position: absolute; top: 9px; left: 10px; width: 16px; height: 16px; color: #8d9097; }
        .search input { padding-left: 34px; color: #ffffff; background: #1f2125; border-color: #303237; }
        .search input::placeholder { color: #8d9097; }
        .search input:focus { background: var(--surface); color: var(--text); outline-color: rgba(0, 0, 0, 0.25); }
        .bar-right { display: flex; align-items: center; gap: 6px; margin-left: auto; }
        .icon-btn { position: relative; padding: 8px; color: #e7e8ea; background: transparent; border: 0; border-radius: 6px; }
        .icon-btn:hover { background: #1f2125; }
        .count { position: absolute; top: 2px; right: 0; min-width: 17px; padding: 0 4px; font-size: 0.72rem; font-weight: 700; line-height: 17px; color: #ffffff; background: var(--red); border-radius: 9px; }
        .me-btn { display: flex; align-items: center; gap: 8px; padding: 4px 8px; color: #ffffff; background: transparent; border: 0; font-weight: 400; }
        .me-btn:hover { background: #1f2125; }
        .avatar { display: inline-flex; align-items: center; justify-content: center; width: 30px; height: 30px; font-size: 0.85rem; font-weight: 700; color: #ffffff; background: #3a3d42; border-radius: 50%; flex: none; }
        .avatar.alt { background: #4a4d55; }
        .avatar.sys { color: var(--muted); background: var(--gray-soft); }
        .me-text { text-align: left; line-height: 1.2; }
        .me-text small { display: block; color: #8d9097; font-size: 0.8rem; }
        .dropdown { position: absolute; top: 50px; right: 12px; z-index: 20; width: 340px; max-height: 70vh; overflow: auto; color: var(--text); background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 12px 32px rgba(0, 0, 0, 0.18); }
        .dropdown h3 { margin: 0; padding: 12px 14px; font-size: 1rem; border-bottom: 1px solid var(--line); }
        .dropdown .item { display: block; padding: 10px 14px; color: var(--text); border-bottom: 1px solid var(--rule); }
        .dropdown .item:hover { background: var(--soft); text-decoration: none; }
        .dropdown .item small { display: block; color: var(--muted); }
        .dropdown .menu-row { display: flex; align-items: center; gap: 8px; width: 100%; padding: 10px 14px; color: var(--text); background: none; border: 0; border-radius: 0; font-weight: 400; }
        .dropdown .menu-row:hover { background: var(--soft); }

        .frame { display: grid; grid-template-columns: 232px minmax(0, 1fr); min-height: calc(100vh - 54px); background: linear-gradient(to right, var(--nav) 232px, transparent 232px); }
        .sidenav { display: flex; flex-direction: column; gap: 2px; padding: 14px 10px; color: var(--nav-text); background: var(--nav); }
        @media (min-width: 961px) { .sidenav { position: sticky; top: 54px; height: calc(100vh - 54px); overflow: auto; } }
        .sidenav .div { height: 1px; margin: 10px; background: #26282d; }
        .sidenav a { display: flex; align-items: center; gap: 10px; padding: 8px 10px; color: var(--nav-text); border-radius: 6px; }
        .sidenav a:hover { color: #ffffff; background: rgba(255, 255, 255, 0.06); text-decoration: none; }
        .sidenav a.on { color: #ffffff; background: rgba(255, 255, 255, 0.1); box-shadow: inset 3px 0 0 #ffffff; }
        .sidenav a .n { margin-left: auto; padding: 0 7px; font-size: 0.78rem; font-weight: 700; color: #ffffff; background: #3a3d42; border-radius: 9px; }
        .sidenav .clock { margin: auto 10px 0; padding-top: 14px; font-size: 0.85rem; color: #8d9097; border-top: 1px solid #26282d; }
        .clock-btns { display: flex; gap: 4px; margin-top: 6px; }
        .sidenav .clock b { color: #e7e8ea; font-weight: 600; }
        .sidenav .clock button { margin: 0; padding: 3px 8px; font-size: 0.8rem; color: #e7e8ea; background: #1f2125; border-color: #3a3d42; }
        main.content { padding: 22px 26px 40px; max-width: 1320px; }
        @media (max-width: 960px) {
            .frame { grid-template-columns: 1fr; background: none; }
            .sidenav { position: fixed; top: 54px; bottom: 0; left: 0; z-index: 15; width: 250px; overflow: auto; transform: translateX(-100%); transition: transform 0.2s; }
            .sidenav.open { transform: none; }
            .topbar .menu { display: inline-flex; }
            .topbar .logo { min-width: 0; }
            .topbar .logo small, .me-text { display: none; }
            main.content { padding: 16px; }
        }

        /* ---------- page furniture ---------- */
        .page-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 18px; flex-wrap: wrap; }
        .page-head h1 { margin: 0; font-size: 1.55rem; font-weight: 650; }
        .page-head p { margin: 2px 0 0; color: var(--muted); }
        .card { margin-bottom: 18px; background: var(--card); border: 1px solid var(--line); border-radius: var(--radius); box-shadow: var(--shadow); }
        .card-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; padding: 12px 16px; border-bottom: 1px solid var(--line); }
        .card-head h2 { margin: 0; font-size: 1.02rem; font-weight: 650; }
        .card-body { padding: 16px; }
        .grid-2 { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(280px, 1fr); gap: 18px; align-items: start; }
        @media (max-width: 1100px) { .grid-2 { grid-template-columns: 1fr; } }
        .hint { color: var(--muted); }
        .empty { padding: 26px 16px; text-align: center; color: var(--muted); }

        /* KPI tiles */
        .tiles { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 14px; margin-bottom: 18px; }
        .tile { display: flex; gap: 12px; align-items: center; padding: 14px 16px; color: var(--text); background: var(--card); border: 1px solid var(--line); border-radius: var(--radius); box-shadow: var(--shadow); }
        a.tile:hover { border-color: var(--brand-line); text-decoration: none; }
        .tile .ic { display: inline-flex; padding: 9px; border-radius: 8px; color: var(--brand); background: var(--brand-soft); }
        .tile .ic.red { color: var(--red); background: var(--red-soft); }
        .tile .ic.amber { color: var(--amber); background: var(--amber-soft); }
        .tile .ic.green { color: var(--green); background: var(--green-soft); }
        .tile .ic.purple { color: var(--purple); background: var(--purple-soft); }
        .tile b { display: block; font-size: 1.5rem; line-height: 1.1; }
        .tile span { color: var(--muted); font-size: 0.92rem; }

        /* priority and state labels */
        .pri { display: inline-flex; align-items: center; gap: 5px; padding: 1px 8px; font-size: 0.85rem; font-weight: 600; border-radius: 10px; white-space: nowrap; }
        .pri::before { content: ""; width: 7px; height: 7px; border-radius: 50%; background: currentColor; }
        .pri.CRITICAL { color: var(--red); background: var(--red-soft); }
        .pri.HIGH { color: var(--orange); background: var(--orange-soft); }
        .pri.MEDIUM { color: var(--amber); background: var(--amber-soft); }
        .pri.LOW { color: var(--green); background: var(--green-soft); }
        .state { display: inline-block; padding: 1px 8px; font-size: 0.85rem; font-weight: 600; border-radius: 4px; white-space: nowrap; }
        .state.NEW, .state.ASSIGNED { color: var(--blue); background: var(--blue-soft); }
        .state.IN_PROGRESS { color: var(--brand-dark); background: var(--brand-soft); }
        .state.WAITING { color: var(--purple); background: var(--purple-soft); }
        .state.RESOLVED { color: var(--green); background: var(--green-soft); }
        .state.CLOSED { color: var(--label); background: var(--gray-soft); }
        .state.REOPENED { color: var(--orange); background: var(--orange-soft); }
        .tag { display: inline-block; margin-left: 6px; padding: 0 6px; font-size: 0.76rem; font-weight: 700; color: var(--on-brand); background: var(--brand); border-radius: 4px; vertical-align: 1px; }
        .tag.red { background: var(--red); }

        /* SLA meter */
        .sla { min-width: 120px; }
        .sla .txt { font-size: 0.88rem; color: var(--muted); }
        .sla .txt.late { color: var(--red); font-weight: 600; }
        .sla .txt.risk { color: var(--amber); font-weight: 600; }
        .meter { height: 6px; margin-top: 4px; overflow: hidden; background: var(--track); border-radius: 4px; }
        .meter i { display: block; height: 100%; background: var(--green); border-radius: 4px; }
        .meter i.risk { background: #c48a1e; }
        .meter i.late { background: var(--red); }

        /* list views */
        .toolbar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding: 12px 16px; border-bottom: 1px solid var(--line); }
        .toolbar input[data-filter=q] { flex: 1; min-width: 200px; width: auto; }
        .table-wrap { overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; }
        th { padding: 9px 12px; font-size: 0.88rem; font-weight: 600; text-align: left; color: var(--muted); background: var(--soft); border-bottom: 1px solid var(--line); white-space: nowrap; }
        td { padding: 11px 12px; vertical-align: middle; border-bottom: 1px solid var(--rule); }
        tbody tr:last-child td { border-bottom: 0; }
        tr.row { cursor: pointer; }
        tr.row:hover td { background: var(--soft); }
        tr.row.attn td:first-child { box-shadow: inset 3px 0 0 var(--brand); }
        .num { font-weight: 650; white-space: nowrap; }
        .desc { font-weight: 600; color: var(--text); }
        .sub { color: var(--muted); font-size: 0.88rem; }
        .todo { margin-top: 2px; font-size: 0.88rem; font-weight: 600; color: var(--brand-dark); }
        td input, td select { width: auto; min-width: 84px; }
        tr.off td { color: var(--faint); }

        /* incident record */
        .rec-head { display: flex; justify-content: space-between; gap: 16px; padding: 16px 18px; flex-wrap: wrap; }
        .rec-head h1 { margin: 2px 0 8px; font-size: 1.4rem; font-weight: 650; }
        .rec-head .labels { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
        .rec-actions { display: flex; gap: 8px; align-items: flex-start; flex-wrap: wrap; }
        .banner { display: flex; gap: 12px; margin: 0 18px 16px; padding: 12px 14px; background: var(--brand-soft); border: 1px solid var(--brand-line); border-radius: 8px; }
        .banner.alert { background: var(--red-soft); border-color: var(--red-line); }
        .banner.calm { background: var(--soft); border-color: var(--line); }
        .banner svg.i { margin-top: 2px; color: var(--brand); }
        .banner.alert svg.i { color: var(--red); }
        .banner b { display: block; }
        .banner p { margin: 2px 0 0; color: var(--label); }
        .fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 28px; padding: 6px 18px 16px; }
        @media (max-width: 700px) { .fields { grid-template-columns: 1fr; } }
        .field { display: grid; grid-template-columns: 130px 1fr; gap: 10px; padding: 8px 0; border-bottom: 1px solid var(--rule); }
        .field span { color: var(--muted); }
        .tabs { display: flex; gap: 2px; padding: 0 16px; border-bottom: 1px solid var(--line); }
        .tabs button { padding: 10px 12px; color: var(--muted); background: none; border: 0; border-bottom: 2px solid transparent; border-radius: 0; }
        .tabs button.on { color: var(--brand-dark); border-bottom-color: var(--brand); }
        .composer { padding: 14px 16px; border-bottom: 1px solid var(--line); }
        .composer.note textarea { background: var(--note-bg); border-color: var(--note-line); }
        .composer .foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 8px; flex-wrap: wrap; }
        .stream { margin: 0; padding: 6px 16px 14px; list-style: none; }
        .entry { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--rule); }
        .entry:last-child { border-bottom: 0; }
        .entry .bubble { flex: 1; min-width: 0; }
        .entry .who { font-size: 0.92rem; color: var(--muted); }
        .entry .who b { color: var(--text); }
        .entry .kind { display: inline-block; margin-left: 6px; padding: 0 6px; font-size: 0.78rem; font-weight: 600; border-radius: 4px; color: var(--blue); background: var(--blue-soft); }
        .entry .kind.note { color: var(--amber); background: var(--amber-soft); }
        .entry .kind.fix { color: var(--green); background: var(--green-soft); }
        .entry .body { margin-top: 4px; padding: 9px 12px; white-space: pre-wrap; background: var(--soft); border: 1px solid var(--line); border-radius: 8px; }
        .entry.note .body { background: var(--note-bg); border-color: var(--note-line); }
        .entry.fix .body { background: var(--fix-bg); border-color: var(--fix-line); }
        .entry.event { align-items: center; padding: 7px 0; font-size: 0.92rem; color: var(--muted); }
        .entry.event .dot { width: 30px; text-align: center; color: var(--faint); }
        .kv { margin: 0; }
        .kv div { display: flex; justify-content: space-between; gap: 10px; padding: 7px 0; border-bottom: 1px solid var(--rule); }
        .kv div:last-child { border-bottom: 0; }
        .kv dt { color: var(--muted); }
        .kv dd { margin: 0; text-align: right; }

        /* report an incident */
        .catalog { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 12px; }
        .svc { display: flex; gap: 10px; align-items: flex-start; padding: 14px; text-align: left; color: var(--text); background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); font-weight: 400; white-space: normal; }
        .svc:hover { background: var(--soft); border-color: var(--brand-line); }
        .svc.on { background: var(--brand-soft); border-color: var(--brand); box-shadow: inset 0 0 0 1px var(--brand); }
        .svc .ic { display: inline-flex; padding: 8px; color: var(--brand); background: var(--brand-soft); border-radius: 8px; }
        .svc.on .ic { color: var(--on-brand); background: var(--brand); }
        .svc b { display: block; }
        .svc small { color: var(--muted); }
        .choices { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 8px; }
        .choice { display: block; padding: 10px 12px; text-align: left; color: var(--text); background: var(--surface); border: 1px solid var(--field-line); border-radius: 6px; font-weight: 400; white-space: normal; }
        .choice:hover { background: var(--soft); }
        .choice.on { background: var(--brand-soft); border-color: var(--brand); box-shadow: inset 0 0 0 1px var(--brand); }
        .choice b { display: block; }
        .choice small { color: var(--muted); }

        /* charts */
        .bars div { display: grid; grid-template-columns: 150px 1fr 34px; align-items: center; gap: 10px; margin: 8px 0; }
        .bars span:first-child { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .bars i { display: block; height: 10px; background: var(--brand); border-radius: 5px; }
        .bars span:last-child { text-align: right; font-weight: 600; }

        /* ---------- streamlined layout ---------- */
        .narrow { max-width: 980px; }
        .back { display: inline-block; margin-bottom: 10px; font-weight: 600; }
        .chips { display: flex; flex-wrap: wrap; gap: 8px; margin: -6px 0 14px; }
        .chip { padding: 5px 12px; font-size: 0.92rem; font-weight: 600; color: var(--text); background: var(--surface); border: 1px solid var(--line); border-radius: 16px; }
        .chip:hover { border-color: var(--brand-line); text-decoration: none; }
        .chip.on { color: var(--on-brand); background: var(--brand); border-color: var(--brand); }
        .chip .n { margin-left: 6px; padding: 0 6px; font-size: 0.78rem; color: var(--on-brand); background: var(--brand); border-radius: 9px; }
        .chip.on .n { color: var(--brand); background: var(--surface); }
        .filter-panel { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 8px 12px; padding: 12px 16px; background: var(--soft); border-bottom: 1px solid var(--line); }
        .filter-panel label { margin: 0 0 4px; font-size: 0.85rem; }
        button.ghost.on { color: var(--brand-dark); background: var(--brand-soft); border-color: var(--brand-line); }
        .sub .todo { display: inline; margin: 0; color: var(--brand-dark); font-weight: 600; }
        .slachip { display: inline-flex; align-items: center; gap: 4px; padding: 1px 8px; font-size: 0.85rem; font-weight: 600; color: var(--muted); background: var(--gray-soft); border-radius: 10px; }
        .slachip.risk { color: var(--amber); background: var(--amber-soft); }
        .slachip.late { color: var(--red); background: var(--red-soft); }
        .slachip svg.i { width: 14px; height: 14px; }
        .status-line { display: flex; gap: 8px; align-items: flex-start; margin: 0 18px 12px; color: var(--muted); }
        .status-line b { color: var(--text); font-weight: 600; }
        .meta { padding: 0 18px 12px; color: var(--muted); font-size: 0.95rem; }
        .meta.related { margin-top: -6px; }
        .meta svg.i { width: 15px; height: 15px; vertical-align: -3px; }
        .callout { display: flex; gap: 10px; align-items: flex-start; margin: 0 18px 16px; padding: 10px 12px; background: var(--fix-bg); border: 1px solid var(--fix-line); border-radius: 8px; }
        .callout > div { flex: 1; min-width: 0; }
        .callout svg.i { margin-top: 2px; color: var(--green); }
        .clamp { display: -webkit-box; margin: 2px 0 0; overflow: hidden; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
        .drawer { padding: 12px 18px; background: var(--soft); border-top: 1px solid var(--line); }
        .mrow { display: grid; grid-template-columns: 110px 1fr auto; gap: 8px; align-items: center; margin-bottom: 8px; }
        .mrow label { margin: 0; }
        .mwarn { display: flex; gap: 10px; align-items: center; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--line); flex-wrap: wrap; }
        @media (max-width: 600px) { .mrow { grid-template-columns: 1fr; } }
        .toggle { display: inline-flex; padding: 2px; background: var(--gray-soft); border-radius: 6px; }
        .toggle button { padding: 4px 10px; font-size: 0.88rem; color: var(--muted); background: transparent; border: 0; }
        .toggle button:hover { background: transparent; color: var(--text); }
        .toggle button.on { color: var(--text); background: var(--surface); box-shadow: var(--shadow); }
        .composer textarea { min-height: 0; height: 64px; transition: height 0.15s; }
        .composer textarea:focus, .composer textarea:not(:placeholder-shown) { height: 120px; }
        .composer.ask textarea { border-color: var(--brand); }
        .composer .foot .hint { visibility: hidden; }
        .composer:focus-within .foot .hint { visibility: visible; }
        .entry.more { justify-content: center; gap: 6px; }
        .tabs a { padding: 10px 12px; font-weight: 600; color: var(--muted); border-bottom: 2px solid transparent; }
        .tabs a.on { color: var(--brand-dark); border-bottom-color: var(--brand); }
        .tabs a:hover { color: var(--text); text-decoration: none; }
        .page-tabs { margin: -8px 0 14px; padding: 0; overflow-x: auto; }
        .page-hint { margin: 0 0 14px; }
        .grid-eq { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; align-items: start; }
        @media (max-width: 1100px) { .grid-eq { grid-template-columns: 1fr; } }
        .tiles.t3 { grid-template-columns: repeat(3, minmax(0, 1fr)); }
        .tiles.t4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
        @media (max-width: 900px) { .tiles.t4 { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
        @media (max-width: 700px) { .tiles.t3, .tiles.t4 { grid-template-columns: 1fr; } }
        .workload { min-width: 180px; }
        .workload small { display: block; color: var(--muted); }
        .workload .meter { width: 160px; height: 8px; }
        .add-form { background: var(--soft); border-bottom: 1px solid var(--line); }
        .add-row { display: grid; grid-template-columns: 2fr 1fr 110px auto; gap: 8px; align-items: end; }
        .add-row label { margin-top: 0; }
        @media (max-width: 760px) { .add-row { grid-template-columns: 1fr; } }
        .picked { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
        .picked .svc { cursor: default; }

        /* dialogs and toasts */
        #modal { position: fixed; inset: 0; z-index: 40; display: flex; align-items: center; justify-content: center; padding: 16px; background: rgba(0, 0, 0, 0.55); }
        #modal .dialog { width: 100%; max-width: 540px; max-height: 88vh; overflow: auto; padding: 20px 22px; background: var(--surface); border-radius: 10px; box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3); }
        #modal h2 { margin: 0 0 4px; font-size: 1.15rem; }
        .rating { display: flex; gap: 6px; }
        .rating button { min-width: 42px; color: var(--text); background: var(--surface); border-color: var(--field-line); }
        .rating button.on { color: var(--on-brand); background: var(--brand); border-color: var(--brand); }
        #toast { position: fixed; right: 20px; bottom: 20px; z-index: 50; display: none; max-width: 420px; padding: 11px 16px; color: #ffffff; background: #131416; border-left: 4px solid #5db98a; border-radius: 8px; box-shadow: 0 10px 24px rgba(0, 0, 0, 0.2); }
        #toast.bad { border-left-color: #e36a62; }
    </style>
</head>
""";

        static final String BODY = """
<body>
    <!-- ======================== sign in ======================== -->
    <div id="loginView" class="auth hidden">
        <section class="auth-art">
            <div class="mark"><span class="logo-box"><svg class="i" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg></span><div><b>I-ITSM</b><small>Campus Service Desk</small></div></div>
            <h2>Report problems with campus IT services and follow them until they are fixed</h2>
            <div class="auth-foot"><span class="org">Computer Centre and IT Services (CCIT)</span><button type="button" class="theme-switch" aria-pressed="false">Dark mode<span class="switch" aria-hidden="true"></span></button></div>
        </section>
        <section class="auth-form">
            <div class="auth-box" id="signinBox">
                <span class="pill">Demo environment</span>
                <h1>I-ITSM</h1>
                <p class="sub">Campus Service Desk</p>
                <span class="caps" id="wsLabel">Choose a workspace</span>
                <div class="seg" role="radiogroup" aria-labelledby="wsLabel">
                    <button type="button" data-role="student" data-fill="harshit|gvgB56UtQk" class="on" role="radio" aria-checked="true"><span class="radio" aria-hidden="true"></span><span><b>Student</b><small>Raise and track your own campus IT issues</small></span><span class="acct">Demo user<strong>Harshit</strong></span></button>
                    <button type="button" data-role="tech" data-fill="harshitha|46JhbvEvcK" role="radio" aria-checked="false"><span class="radio" aria-hidden="true"></span><span><b>Support staff</b><small>Pick up tickets, add notes and resolve issues</small></span><span class="acct">Demo user<strong>Harshitha</strong></span></button>
                    <button type="button" data-role="admin" data-fill="harshini|72r0B2HVWz" role="radio" aria-checked="false"><span class="radio" aria-hidden="true"></span><span><b>Admin</b><small>Oversee queues, deadlines and escalations</small></span><span class="acct">Demo user<strong>Harshini</strong></span></button>
                </div>
                <label for="lUser">Username</label>
                <input id="lUser" autocomplete="username" placeholder="Username or campus email">
                <label for="lPass">Password</label>
                <input id="lPass" type="password" autocomplete="current-password">
                <button class="wide" id="signinBtn">Sign in<svg class="i" viewBox="0 0 24 24"><path d="M5 12h14M13 6l6 6-6 6"/></svg></button>
                <div class="error" id="authMsg" role="alert"></div>
                <p class="fine">Click a workspace to fill in its demo user.</p>
                <div class="auth-alt" id="registerLink">New student? <a href="#" data-auth="register">Create an account</a></div>
            </div>
            <div class="auth-box hidden" id="registerBox">
                <h1>Create a student account</h1>
                <p class="sub">Use your campus email so CCIT can contact you about your incidents.</p>
                <label for="rName">Full name</label>
                <input id="rName" autocomplete="name">
                <label for="rEmail">Campus email</label>
                <input id="rEmail" type="email" autocomplete="email">
                <div class="row-2">
                    <div><label for="rUser">Username</label><input id="rUser" autocomplete="username"></div>
                    <div><label for="rPass">Password</label><input id="rPass" type="password" autocomplete="new-password" placeholder="6+ characters"></div>
                </div>
                <button class="wide" id="registerBtn">Create account</button>
                <div class="auth-alt">Already registered? <a href="#" data-auth="signin">Sign in</a></div>
                <div class="error" id="regMsg" role="alert"></div>
            </div>
        </section>
    </div>

    <!-- ======================== application ======================== -->
    <div id="appView" class="hidden">
        <header class="topbar">
            <button class="menu" id="menuBtn" aria-label="Open navigation"><svg class="i" viewBox="0 0 24 24"><path d="M3 6h18M3 12h18M3 18h18"/></svg></button>
            <a class="logo" href="#home"><svg class="i" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>I-ITSM <small>Service Desk</small></a>
            <div class="search">
                <svg class="i" viewBox="0 0 24 24"><path d="M11 19a8 8 0 100-16 8 8 0 000 16zM21 21l-4.3-4.3"/></svg>
                <input id="globalSearch" placeholder="Search incidents by number or words" aria-label="Search incidents">
            </div>
            <div class="bar-right">
                <button class="icon-btn theme-toggle theme-switch" id="themeBtn" aria-label="Dark mode" aria-pressed="false"><svg class="i moon" viewBox="0 0 24 24"><path d="M21 12.8A9 9 0 1111.2 3a7 7 0 009.8 9.8z"/></svg><svg class="i sun" viewBox="0 0 24 24"><path d="M12 17a5 5 0 100-10 5 5 0 000 10zM12 1v2M12 21v2M4.2 4.2l1.4 1.4M18.4 18.4l1.4 1.4M1 12h2M21 12h2M4.2 19.8l1.4-1.4M18.4 5.6l1.4-1.4"/></svg></button>
                <button class="icon-btn" id="bellBtn" aria-label="Notifications"><svg class="i" viewBox="0 0 24 24"><path d="M18 8a6 6 0 00-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 01-3.4 0"/></svg><span class="count hidden" id="bellCount"></span></button>
                <button class="me-btn" id="meBtn"><span class="avatar" id="meAvatar"></span><span class="me-text"><span id="meName"></span><small id="meRole"></small></span></button>
            </div>
            <div class="dropdown hidden" id="bellMenu"></div>
            <div class="dropdown hidden" id="meMenu" style="width:240px"></div>
        </header>
        <div class="frame">
            <nav class="sidenav" id="sidenav" aria-label="Main"></nav>
            <main class="content" id="view"></main>
        </div>
    </div>

    <div id="modal" class="hidden" role="dialog" aria-modal="true"><div class="dialog" id="modalBox"></div></div>
    <div id="toast" role="status"></div>
""";

        static final String SCRIPT1 = """
    <script>
        // ---------- light and dark theme ----------
        function isDark() {
            const set = document.documentElement.dataset.theme;
            return set ? set === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
        }
        function syncTheme() {
            const dark = isDark();
            document.documentElement.dataset.dark = String(dark);
            document.querySelectorAll('.theme-switch').forEach(function (b) {
                b.setAttribute('aria-pressed', String(dark));
                if (b.id === 'themeBtn') { b.setAttribute('aria-label', dark ? 'Switch to light mode' : 'Switch to dark mode'); }
            });
        }
        document.querySelectorAll('.theme-switch').forEach(function (b) {
            b.addEventListener('click', function () {
                const next = isDark() ? 'light' : 'dark';
                document.documentElement.dataset.theme = next;
                try { localStorage.setItem('itsm-theme', next); } catch (e) { /* storage blocked: lasts until reload */ }
                syncTheme();
            });
        });
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', syncTheme);
        syncTheme();

        // ======================================================================
        // Shared state and helpers
        // ======================================================================
        let me = null;          // the signed-in user
        let state = null;       // the latest /api/state reply
        let ticket = null;      // the latest /api/ticket reply on a record page
        let route = { page: 'home', arg: '' };
        let filters = { q: '', status: '', priority: '', service: '', sla: '', tech: '', from: '' };
        let composeMode = 'message';
        let authRole = 'student';
        let newOnVisit = {};
        let loadSeq = 0;
        let authSeq = 0;
        // What is open or collapsed on screen; only the activity view is remembered between visits.
        let ui = { filters: false, details: false, manage: false, stream: store('itsm-stream') || 'conv', addTech: false, addSvc: false, svcOpen: false };
        let cameFrom = '';

        const PRIORITY = { CRITICAL: 'P1 Critical', HIGH: 'P2 High', MEDIUM: 'P3 Moderate', LOW: 'P4 Low' };
        const STATE_NAME = { NEW: 'New', ASSIGNED: 'Assigned', IN_PROGRESS: 'In progress', WAITING: 'Awaiting student', RESOLVED: 'Resolved', CLOSED: 'Closed', REOPENED: 'Reopened' };

        const ICONS = {
            home: 'M3 11l9-8 9 8M5 10v10h14V10',
            plus: 'M12 5v14M5 12h14',
            list: 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01',
            alert: 'M12 9v4M12 17h.01M10.3 3.9L1.8 18a2 2 0 001.7 3h17a2 2 0 001.7-3L13.7 3.9a2 2 0 00-3.4 0z',
            inbox: 'M22 12h-6l-2 3h-4l-2-3H2M5.5 5h13l3.5 7v6a2 2 0 01-2 2H4a2 2 0 01-2-2v-6z',
            check: 'M20 6L9 17l-5-5',
            chart: 'M3 3v18h18M7 15l4-4 3 3 5-6',
            users: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 11a4 4 0 100-8 4 4 0 000 8zM23 21v-2a4 4 0 00-3-3.9M16 3.1a4 4 0 010 7.8',
            grid: 'M3 3h7v7H3zM14 3h7v7h-7zM14 14h7v7h-7zM3 14h7v7H3z',
            clock: 'M12 22a10 10 0 100-20 10 10 0 000 20zM12 6v6l4 2',
            file: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6M8 13h8M8 17h8',
            up: 'M12 19V5M5 12l7-7 7 7',
            fire: 'M12 22c4 0 7-3 7-7 0-4-3-6-4-9-2 2-3 4-3 6-1-1-2-2-2-4-3 2-5 5-5 7 0 4 3 7 7 7z',
            star: 'M12 2l3 7 7 .6-5.3 4.7 1.6 7.2L12 17.8 5.7 21.5l1.6-7.2L2 9.6 9 9z',
            wifi: 'M5 12.5a10 10 0 0114 0M8.5 16a5 5 0 017 0M12 19.5h.01M1.5 9a15 15 0 0121 0',
            building: 'M3 21h18M5 21V7l7-4 7 4v14M9 9h1M14 9h1M9 13h1M14 13h1M9 17h1M14 17h1',
            globe: 'M12 22a10 10 0 100-20 10 10 0 000 20zM2 12h20M12 2a15 15 0 010 20M12 2a15 15 0 000 20',
            mail: 'M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zM22 6l-10 7L2 6',
            lock: 'M5 11h14v10H5zM8 11V7a4 4 0 018 0v4',
            book: 'M4 19.5A2.5 2.5 0 016.5 17H20V3H6.5A2.5 2.5 0 004 5.5zM4 19.5V21h16',
            help: 'M12 22a10 10 0 100-20 10 10 0 000 20zM9.1 9a3 3 0 015.8 1c0 2-3 3-3 3M12 17h.01',
            plug: 'M9 2v6M15 2v6M6 8h12v4a6 6 0 01-12 0zM12 18v4',
            logout: 'M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4M16 17l5-5-5-5M21 12H9',
            message: 'M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z',
            info: 'M12 22a10 10 0 100-20 10 10 0 000 20zM12 16v-4M12 8h.01',
            sliders: 'M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6',
            dot: 'M12 13a1 1 0 100-2 1 1 0 000 2z',
            link: 'M10 13a5 5 0 007.5.5l3-3a5 5 0 00-7-7l-1.7 1.7M14 11a5 5 0 00-7.5-.5l-3 3a5 5 0 007 7l1.7-1.7'
        };
        const SERVICE_ICON = { wifi: 'wifi', 'hostel-room': 'plug', 'campus-network': 'globe', vpn: 'lock', lms: 'book', website: 'globe', email: 'mail', other: 'help' };
        const SERVICE_HINT = {
            wifi: 'Wi-Fi in hostel rooms, corridors and common rooms',
            'hostel-room': 'LAN ports, power sockets and fittings in your room',
            'campus-network': 'Wi-Fi and internet in academic blocks and the library',
            vpn: 'Reaching campus systems from outside',
            lms: 'Quizzes, assignments, the student portal',
            website: 'Pages, forms and links on the campus website',
            email: 'Sending, receiving and syncing campus mail',
            other: 'Anything that does not fit the list'
        };

        function icon(name) {
            return '<svg class="i" viewBox="0 0 24 24" aria-hidden="true"><path d="' + (ICONS[name] || ICONS.dot) + '"/></svg>';
        }
        function esc(value) {
            return String(value == null ? '' : value).replace(/[&<>"']/g, function (c) {
                return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
            });
        }
        function byId(id) { return document.getElementById(id); }
        function show(id, visible) { const el = byId(id); if (el) { el.classList.toggle('hidden', !visible); } }
        function lowerFirst(t) { return t ? t.charAt(0).toLowerCase() + t.slice(1) : t; }
        function initials(name) {
            return String(name || '?').split(/[^A-Za-z0-9]+/).filter(Boolean).slice(0, 2).map(function (w) { return w.charAt(0).toUpperCase(); }).join('') || '?';
        }
        function store(key, value) { try { if (value === undefined) { return localStorage.getItem(key); } localStorage.setItem(key, value); } catch (e) { return null; } return null; }

        // Replaces an element's HTML only when it changed, and never while someone is typing inside it.
        function setHtml(id, html) {
            const el = byId(id);
            if (!el || el.dataset.sig === html) { return; }
            const a = document.activeElement;
            if (a && el.contains(a) && (a.tagName === 'INPUT' || a.tagName === 'TEXTAREA' || a.tagName === 'SELECT')) { return; }
            el.innerHTML = html;
            el.dataset.sig = html;
        }

        function toast(message, isError) {
            const box = byId('toast');
            box.textContent = message;
            box.classList.toggle('bad', !!isError);
            box.style.display = 'block';
            clearTimeout(window.toastTimer);
            window.toastTimer = setTimeout(function () { box.style.display = 'none'; }, 5000);
        }

        async function post(url, data) {
            try {
                const response = await fetch(url, { method: 'POST', body: new URLSearchParams(data) });
                const result = await response.json();
                if (response.status === 401) { showLogin(); return result; }
                toast(result.ok ? result.message : result.error, !result.ok);
                load();
                return result;
            } catch (e) {
                toast('The service desk server is not responding. Check that it is running, then try again.', true);
                return { ok: false };
            }
        }

        function priorityPill(p) { return '<span class="pri ' + esc(p) + '">' + esc(PRIORITY[p] || p) + '</span>'; }
        function stateBadge(t) { return '<span class="state ' + esc(t.stateKey) + '">' + esc(STATE_NAME[t.stateKey] || t.stateKey) + '</span>'; }
        function slaCell(t) {
            if (t.timeLeft === 'Done') { return '<div class="sla"><div class="txt">Completed</div></div>'; }
            const pct = Math.min(100, Math.round(t.slaPct));
            const cls = t.sla === 'BREACHED' ? 'late' : t.sla === 'AT_RISK' ? 'risk' : '';
            const label = t.sla === 'BREACHED' ? 'Breached, ' + t.timeLeft : t.sla === 'AT_RISK' ? 'At risk, ' + t.timeLeft : t.timeLeft;
            return '<div class="sla"><div class="txt ' + cls + '">' + esc(label) + '</div><div class="meter"><i class="' + cls + '" style="width:' + pct + '%"></i></div></div>';
        }
        function needsYou(t) { return !!t.needsAction || t.unread > 0; }
        function isOpen(t) { return t.stateKey !== 'RESOLVED' && t.stateKey !== 'CLOSED'; }

        // ======================================================================
        // Sign in and registration
        // ======================================================================
        function renderAuth() {
            document.querySelectorAll('.seg button').forEach(function (b) {
                b.classList.toggle('on', b.dataset.role === authRole);
                b.setAttribute('aria-checked', String(b.dataset.role === authRole));
            });
            show('registerLink', authRole === 'student');
            byId('authMsg').textContent = '';
        }
        function showLogin() {
            me = null;
            show('appView', false);
            show('loginView', true);
            renderAuth();
        }
        async function authPost(url, data, msgId, btnId) {
            const btn = byId(btnId);
            btn.disabled = true;
            try {
                const response = await fetch(url, { method: 'POST', body: new URLSearchParams(data) });
                const result = await response.json();
                if (result.ok) {
                    authSeq++;
                    byId(msgId).textContent = '';
                    byId('lPass').value = '';
                    byId('rPass').value = '';
                    history.replaceState(null, '', '#home');
                    route = { page: 'home', arg: '' };
                    cameFrom = '#home';
                    ui.filters = false;
                    resetFilters();
                    await load(true);
                    toast(result.message, false);
                } else {
                    byId(msgId).textContent = result.error;
                }
            } catch (e) {
                byId(msgId).textContent = 'The service desk server is not responding.';
            }
            btn.disabled = false;
        }
        function signIn() { authPost('/api/login', { username: byId('lUser').value, password: byId('lPass').value, role: authRole }, 'authMsg', 'signinBtn'); }
        function registerAccount() {
            authPost('/api/register', { name: byId('rName').value, email: byId('rEmail').value, username: byId('rUser').value, password: byId('rPass').value }, 'regMsg', 'registerBtn');
        }
        async function signOut() {
            authSeq++;
            await fetch('/api/logout', { method: 'POST' });
            show('meMenu', false);
            showLogin();
        }
        byId('signinBtn').onclick = signIn;
        byId('registerBtn').onclick = registerAccount;
        byId('lPass').addEventListener('keydown', function (e) { if (e.key === 'Enter') { signIn(); } });

        // ======================================================================
        // Navigation: every page has its own address, so Back works
        // ======================================================================
        function parseRoute() {
            const h = location.hash.replace(/^#/, '');
            const parts = h.split('/');
            return { page: parts[0] || 'home', arg: parts.slice(1).join('/') };
        }
        function go(hash) {
            if (location.hash === hash) { onRoute(); } else { location.hash = hash; }
        }
        function onRoute() {
            const next = parseRoute();
            const changedPage = next.page !== route.page || next.arg !== route.arg;
            route = next;
            if (changedPage && route.page === 'ticket') { ticket = null; newOnVisit = {}; composeMode = 'message'; ui.details = false; ui.manage = false; }
            if (next.page === 'home' || next.page === 'list') { cameFrom = '#' + next.page + (next.arg ? '/' + next.arg : ''); }
            if (next.page !== 'list' && next.page !== 'ticket') { resetFilters(); ui.filters = false; }
            byId('sidenav').classList.remove('open');
            if (!me) { return; }
            buildView();
            load();
            window.scrollTo(0, 0);
        }
        window.addEventListener('hashchange', onRoute);
        function resetFilters() { filters = { q: '', status: '', priority: '', service: '', sla: '', tech: '', from: '' }; }

        // The menu lists places, not filters: views of one list are chips on that list page.
        function navItems() {
            const c = state.counts || {};
            if (state.role === 'student') {
                return [['home', '#home', 'Home', 'home'], ['report', '#report', 'Report an incident', 'plus'], ['list', '#list/active', 'My incidents', 'list', c.attention]];
            }
            if (state.role === 'tech') {
                return [['home', '#home', 'Home', 'home'], ['list', '#list/assigned', 'My queue', 'inbox', c.attention]];
            }
            return [['home', '#home', 'Home', 'home'], ['list', '#list/open', 'Incidents', 'list', c.attention], ['-'],
                ['reports', '#reports', 'Reports', 'chart'], ['settings', '#people', 'Settings', 'sliders']];
        }
        function navKey() {
            const p = route.page;
            if (p === 'ticket') { return cameFrom.indexOf('#home') === 0 ? 'home' : 'list'; }
            if (p === 'people' || p === 'services' || p === 'sla') { return 'settings'; }
            if (p === 'reports' || p === 'workload' || p === 'audit') { return 'reports'; }
            return p;
        }
        function renderShell() {
            byId('meName').textContent = me.name;
            byId('meRole').textContent = { student: 'Student', tech: 'Technician', admin: 'Administrator' }[me.role];
            byId('meAvatar').textContent = initials(me.name);
            const current = navKey();
            const html = navItems().map(function (n) {
                if (n[0] === '-') { return '<div class="div"></div>'; }
                const badge = n[4] ? '<span class="n">' + n[4] + '</span>' : '';
                return '<a href="' + n[1] + '" class="' + (n[0] === current ? 'on' : '') + '">' + icon(n[3]) + esc(n[2]) + badge + '</a>';
            }).join('') + '<div class="clock"><div>Demo clock &middot; <b>' + esc(state.clock) + '</b></div>'
                + '<div class="clock-btns"><button data-skip="2">+2 min</button><button data-skip="5">+5 min</button><button data-skip="15">+15 min</button></div></div>';
            setHtml('sidenav', html);
            renderBell();
        }

        // ---------- shared pieces used by several pages ----------
        function slaChip(t) {
            if (!t.timeLeft || t.timeLeft === 'Done') { return ''; }
            const cls = t.sla === 'BREACHED' ? 'late' : t.sla === 'AT_RISK' ? 'risk' : '';
            const text = (t.sla === 'BREACHED' ? 'SLA breached: ' : t.sla === 'AT_RISK' ? 'SLA at risk: ' : 'SLA: ') + t.timeLeft;
            return '<span class="slachip ' + cls + '" title="Time left to resolve under the service level agreement">' + icon('clock') + esc(text) + '</span>';
        }
        const COL_LABEL = { num: 'Number', desc: 'Short description', priority: 'Priority', state: 'State', assignee: 'Assigned to', sla: 'SLA', updated: 'Updated' };
        // One table for every incident list: number, a two-line description, then the chosen columns.
        function rowsTable(rows, cols, emptyHtml) {
            if (!rows.length) { return emptyHtml; }
            const staff = state.role !== 'student';
            return '<div class="table-wrap"><table><thead><tr>' + cols.map(function (c) { return '<th>' + COL_LABEL[c] + '</th>'; }).join('') + '</tr></thead><tbody>'
                + rows.map(function (t) {
                    const todo = t.needsAction || (t.unread ? 'New message' : '');
                    const sub = (todo ? '<b class="todo">' + esc(todo) + '</b> &middot; ' : '') + (staff ? esc(t.reporter) + ' &middot; ' : '') + esc(t.service);
                    const cells = cols.map(function (c) {
                        if (c === 'num') { return '<td class="num"><a href="#ticket/' + esc(t.id) + '">' + esc(t.id) + '</a></td>'; }
                        if (c === 'desc') {
                            return '<td><div class="desc">' + esc(t.title) + (t.unread ? '<span class="tag">' + t.unread + ' new</span>' : '')
                                + (t.escalated && isOpen(t) ? '<span class="tag red">Escalated</span>' : '') + '</div><div class="sub">' + sub + '</div></td>';
                        }
                        if (c === 'priority') { return '<td>' + priorityPill(t.priority) + '</td>'; }
                        if (c === 'state') { return '<td>' + stateBadge(t) + '</td>'; }
                        if (c === 'assignee') { return '<td>' + esc(t.assignee) + '</td>'; }
                        if (c === 'sla') { return '<td>' + slaCell(t) + '</td>'; }
                        return '<td class="sub">' + esc(t.updated) + '</td>';
                    }).join('');
                    return '<tr class="row' + (needsYou(t) ? ' attn' : '') + '" data-open="' + esc(t.id) + '">' + cells + '</tr>';
                }).join('') + '</tbody></table></div>';
        }
        const PAGE_TABS = {
            settings: [['people', 'Users and technicians'], ['services', 'Services and categories'], ['sla', 'SLA and assignment']],
            reports: [['reports', 'Overview'], ['workload', 'Technician workload'], ['audit', 'Audit log']]
        };
        function pageTabs(group) {
            return '<nav class="tabs page-tabs">' + PAGE_TABS[group].map(function (t) {
                return '<a href="#' + t[0] + '" class="' + (t[0] === route.page ? 'on' : '') + '">' + esc(t[1]) + '</a>';
            }).join('') + '</nav>';
        }
        function activeFilterCount() {
            return ['status', 'priority', 'service', 'sla', 'tech', 'from'].filter(function (k) { return filters[k]; }).length;
        }
        // Open and close things on screen (filters, details, manage, activity view, add forms).
        function uiClick(what) {
            if (what === 'filters') { ui.filters = !ui.filters; show('lFilters', ui.filters); byId('lFilterBtn').classList.toggle('on', ui.filters); return; }
            if (what === 'clear') { resetFilters(); ui.filters = false; buildView(); load(); return; }
            if (what === 'details') { ui.details = !ui.details; show('rDetailsBox', ui.details); updateTicket(); return; }
            if (what === 'manage') { ui.manage = !ui.manage; updateTicket(); return; }
            if (what === 'conv' || what === 'all') { ui.stream = what; store('itsm-stream', what); updateTicket(); return; }
            if (what === 'addtech') { ui.addTech = !ui.addTech; show('ppAdd', ui.addTech); if (ui.addTech) { byId('tName').focus(); } return; }
            if (what === 'addsvc') { ui.addSvc = !ui.addSvc; show('svAdd', ui.addSvc); if (ui.addSvc) { byId('sName').focus(); } return; }
            if (what === 'svc') { ui.svcOpen = true; byId('catalog').dataset.sig = ''; updateReport(); }
        }

        // ---------- notifications bell ----------
        function renderBell() {
            const items = state.inbox || [];
            const seen = store('itsm-seen-' + me.id) || '';
            let fresh = 0;
            for (let k = 0; k < items.length && items[k] !== seen; k++) { fresh++; }
            byId('bellCount').textContent = fresh > 9 ? '9+' : fresh;
            show('bellCount', fresh > 0);
            const html = '<h3>Notifications</h3>' + (items.length ? items.slice(0, 15).map(function (line, k) {
                const m = line.match(/INC-[0-9]+/);
                const time = line.slice(0, 5);
                const text = line.slice(7);
                return '<a class="item" href="' + (m ? '#ticket/' + m[0] : '#home') + '">' + (k < fresh ? '<span class="tag">New</span> ' : '') + esc(text) + '<small>' + esc(time) + '</small></a>';
            }).join('') : '<div class="empty">You have no notifications.</div>');
            setHtml('bellMenu', html);
        }
        byId('bellBtn').onclick = function (e) {
            e.stopPropagation();
            const open = byId('bellMenu').classList.contains('hidden');
            show('meMenu', false);
            show('bellMenu', open);
            if (open && state && state.inbox.length) { store('itsm-seen-' + me.id, state.inbox[0]); setTimeout(renderBell, 1500); }
        };
        byId('meBtn').onclick = function (e) {
            e.stopPropagation();
            const work = me.role === 'tech'
                ? '<div class="item"><small>Workload</small>' + me.workload + ' of ' + me.capacity + ' open</div><div class="item"><small>Skills</small>' + esc(me.skills.join(', ')) + '</div>' : '';
            byId('meMenu').innerHTML = '<h3>' + esc(me.name) + '<small style="display:block;font-weight:400;color:var(--muted)">' + esc(me.email) + '</small></h3>'
                + work + '<button class="menu-row" id="signOutBtn">' + icon('logout') + 'Sign out</button>';
            byId('signOutBtn').onclick = signOut;
            show('bellMenu', false);
            byId('meMenu').classList.toggle('hidden');
        };
        document.addEventListener('click', function (e) {
            if (!e.target.closest('#bellMenu') && !e.target.closest('#bellBtn')) { show('bellMenu', false); }
            if (!e.target.closest('#meMenu') && !e.target.closest('#meBtn')) { show('meMenu', false); }
        });
        byId('menuBtn').onclick = function () { byId('sidenav').classList.toggle('open'); };
        byId('globalSearch').addEventListener('keydown', function (e) {
            if (e.key !== 'Enter') { return; }
            const v = this.value.trim();
            if (/^INC-[0-9]+$/i.test(v)) { this.value = ''; go('#ticket/' + v.toUpperCase()); return; }
            resetFilters();
            filters.q = v;
            go('#list/' + ({ student: 'mine', tech: 'assigned', admin: 'all' }[state.role]));
        });

        // ---------- dialogs ----------
        function closeModal() { show('modal', false); }
        function form(title, hint, opts) {
            opts = opts || {};
            return new Promise(function (resolve) {
                let rating = 0;
                let html = '<h2>' + esc(title) + '</h2>' + (hint ? '<p class="hint" style="margin:0 0 6px">' + esc(hint) + '</p>' : '');
                if (opts.rating) {
                    html += '<label>Rating <span class="help">1 is poor, 5 is excellent</span></label><div class="rating" id="stars">'
                        + [1, 2, 3, 4, 5].map(function (n) { return '<button data-n="' + n + '">' + n + '</button>'; }).join('') + '</div>';
                }
                html += '<label for="mText">' + esc(opts.label || 'Details') + '</label><textarea id="mText">' + esc(opts.value || '') + '</textarea>'
                    + '<div style="display:flex;gap:8px;margin-top:14px"><button id="mOk">' + esc(opts.ok || 'Save') + '</button><button class="ghost" id="mCancel">Cancel</button></div>';
                byId('modalBox').innerHTML = html;
                show('modal', true);
                byId('mText').focus();
                if (opts.rating) {
                    byId('stars').onclick = function (e) {
                        if (!e.target.dataset.n) { return; }
                        rating = Number(e.target.dataset.n);
                        Array.from(byId('stars').children).forEach(function (b) { b.classList.toggle('on', Number(b.dataset.n) === rating); });
                    };
                }
                byId('mCancel').onclick = function () { closeModal(); resolve(null); };
                byId('mOk').onclick = function () {
                    const text = byId('mText').value;
                    if (opts.rating && !rating) { toast('Choose a rating from 1 to 5.', true); return; }
                    if (!opts.optional && !text.trim()) { toast('This box cannot be empty.', true); return; }
                    closeModal();
                    resolve({ text: text, rating: rating });
                };
            });
        }

        // ---------- one click handler for the whole page ----------
        document.addEventListener('click', function (e) {
            const t = e.target.closest('[data-role],[data-fill],[data-auth],[data-open],[data-action],[data-skip],[data-mode],[data-send],[data-manage],[data-svc],[data-choice],[data-toggle],[data-sla],[data-strategy],[data-ui]');
            if (!t) { return; }
            const d = t.dataset;
            if (d.ui) { e.preventDefault(); uiClick(d.ui); return; }
            if (d.role && t.closest('.seg')) {
                authRole = d.role;
                renderAuth();
                if (d.fill) { const p = d.fill.split('|'); byId('lUser').value = p[0]; byId('lPass').value = p[1]; }
                return;
            }
            if (d.auth) { e.preventDefault(); show('signinBox', d.auth === 'signin'); show('registerBox', d.auth === 'register'); return; }
            if (d.open && e.target.tagName !== 'A' && e.target.tagName !== 'BUTTON') { go('#ticket/' + d.open); return; }
            if (d.action) { act(d.id, d.action, d.prefill); return; }
            if (d.skip) { post('/api/time', { minutes: d.skip }); return; }
            if (d.mode) { composeMode = d.mode; updateTicket(); return; }
            if (d.send) { sendMessage(); return; }
            if (d.manage) { manage(d.manage); return; }
            if (d.svc) { pickService(d.svc); return; }
            if (d.choice) { pickChoice(t); return; }
            if (d.toggle) { post('/api/admin', { op: 'setActive', userId: d.toggle, active: d.active }); return; }
            if (d.sla) { post('/api/admin', { op: 'setSla', priority: d.sla, response: byId('resp-' + d.sla).value, resolution: byId('reso-' + d.sla).value }); return; }
            if (d.strategy) { post('/api/strategy', { name: d.strategy }); return; }
        });
        document.addEventListener('change', function (e) {
            const t = e.target;
            if (t.dataset && t.dataset.crit) { post('/api/admin', { op: 'setCriticality', key: t.dataset.crit, criticality: t.value }); }
            if (t.dataset && t.dataset.filter) { filters[t.dataset.filter] = t.value; load(); }
        });
        document.addEventListener('input', function (e) {
            const t = e.target;
            if (t.dataset && t.dataset.filter === 'q') {
                filters.q = t.value;
                clearTimeout(window.qTimer);
                window.qTimer = setTimeout(load, 300);
            }
        });

        // ======================================================================
        // Loading data
        // ======================================================================
        function stateQuery() {
            if (route.page !== 'list') { return ''; }
            const preset = PRESETS[route.arg] || {};
            const params = new URLSearchParams();
            const merged = Object.assign({}, preset.params || {});
            Object.keys(filters).forEach(function (k) { if (filters[k]) { merged[k] = filters[k]; } });
            Object.keys(merged).forEach(function (k) { if (merged[k]) { params.set(k, merged[k]); } });
            return params.toString();
        }

        // A reply that arrives after a newer request (or a sign-in or sign-out) is ignored.
        async function load(rebuild) {
            const mine = ++loadSeq;
            const authAtStart = authSeq;
            try {
                const response = await fetch('/api/state?' + stateQuery());
                if (mine !== loadSeq || authAtStart !== authSeq) { return; }
                if (response.status === 401) { showLogin(); return; }
                const s = await response.json();
                if (mine !== loadSeq) { return; }
                const first = !me;
                state = s;
                me = s.me;
                show('loginView', false);
                show('appView', true);
                if (first || rebuild) { buildView(); }
                renderShell();
                updateView();
                if (route.page === 'ticket') { loadTicket(); }
            } catch (e) {
                console.error(e);   // the next refresh tries again
            }
        }

        async function loadTicket() {
            const wanted = route.arg;
            try {
                const response = await fetch('/api/ticket?id=' + encodeURIComponent(wanted));
                if (response.status === 401) { showLogin(); return; }
                const d = await response.json();
                if (route.page !== 'ticket' || wanted !== route.arg) { return; }
                if (!d.ok) { toast(d.error, true); go('#home'); return; }
                const first = !ticket;
                ticket = d;
                // the admin's first visit to an overdue or escalated incident opens the Manage drawer once
                if (first && state.role === 'admin' && (d.ticket.needsAction === 'Deadline missed' || d.ticket.needsAction === 'Escalated to you')) { ui.manage = true; }
                updateTicket();
            } catch (e) {
                // try again on the next refresh
            }
        }
""";

        static final String SCRIPT2 = """
        // ======================================================================
        // Pages
        // ======================================================================
        // Each list view is a chip on the one incident list page of a role.
        const PRESETS = {
            active: { chip: 'Open', desc: 'Incidents you reported that are not closed yet.', empty: 'You have no open incidents.', filter: function (t) { return t.stateKey !== 'CLOSED'; } },
            mine: { chip: 'All', desc: 'Everything you have reported to CCIT.', empty: 'You have not reported any incidents yet.' },
            attention: { chip: 'Needs my attention', desc: 'Incidents waiting on you.', empty: 'Nothing needs your attention right now.', filter: needsYou },
            assigned: { chip: 'Open', desc: 'Incidents in your queue that are not closed.', empty: 'Your queue is empty.' },
            sla: { chip: 'At risk or breached', desc: 'Open incidents past 75% of their time, or past the deadline.', empty: 'Nothing is at risk of missing its deadline.', filter: function (t) { return isOpen(t) && t.sla !== 'ON_TRACK'; } },
            closed: { chip: 'Closed', desc: 'Incidents that were fixed and confirmed.', empty: 'No closed incidents yet.', params: { status: 'CLOSED' } },
            late: { chip: 'At risk', desc: 'More than 75% of the time allowed has been used.', empty: 'No incidents are at risk of breach.', params: { sla: 'AT_RISK' } },
            breached: { chip: 'SLA breached', desc: 'Open incidents past their resolution deadline.', empty: 'No incidents have breached their SLA.', params: { sla: 'BREACHED' } },
            all: { chip: 'All', desc: 'Every incident in the service desk.', empty: 'No incidents yet.' },
            open: { chip: 'Open', desc: 'Incidents not yet resolved.', empty: 'No open incidents.', filter: isOpen },
            critical: { chip: 'Critical', desc: 'Open P1 incidents.', empty: 'No open critical incidents.', params: { priority: 'CRITICAL' }, filter: isOpen },
            escalated: { chip: 'Escalated', desc: 'Open incidents escalated to the CCIT admin.', empty: 'No escalated incidents.', filter: function (t) { return t.escalated && isOpen(t); } }
        };
        const CHIPS = { student: ['active', 'attention', 'closed', 'mine'], tech: ['assigned', 'attention', 'sla', 'closed'], admin: ['open', 'attention', 'critical', 'all'] };
        const DEFAULT_LIST = { student: 'active', tech: 'assigned', admin: 'open' };
        const LIST_LABEL = { student: 'My incidents', tech: 'My queue', admin: 'Incidents' };
        const LIST_COLS = { student: ['num', 'desc', 'state', 'updated'], tech: ['num', 'desc', 'priority', 'state', 'sla'], admin: ['num', 'desc', 'priority', 'state', 'assignee', 'sla'] };
        const ADMIN_PAGES = ['people', 'services', 'sla', 'reports', 'workload', 'audit'];

        function buildView() {
            if (!state) { return; }
            if ((ADMIN_PAGES.indexOf(route.page) >= 0 && state.role !== 'admin') || (route.page === 'report' && state.role !== 'student')) {
                history.replaceState(null, '', '#home');
                route = { page: 'home', arg: '' };
            }
            const views = { home: homeView, list: listView, ticket: ticketView, report: reportView, reports: reportsView, workload: workloadView, people: peopleView, services: servicesView, sla: slaView, audit: auditView };
            byId('view').innerHTML = (views[route.page] || homeView)();
        }
        function updateView() {
            const updates = { home: updateHome, list: updateList, report: updateReport, reports: updateReports, workload: updateWorkload, people: updatePeople, services: updateServices, sla: updateSla, audit: updateAudit };
            if (updates[route.page]) { updates[route.page](); }
        }

        function cardHtml(title, inner, right) {
            return '<div class="card"><div class="card-head"><h2>' + esc(title) + '</h2>' + (right ? '<div style="display:flex;gap:14px;align-items:center">' + right + '</div>' : '') + '</div>' + inner + '</div>';
        }
        function card(title, bodyId, extra, right) {
            return cardHtml(title, '<div id="' + bodyId + '"' + (extra || '') + '></div>', right);
        }
        function bars(list, names) {
            let max = 1;
            list.forEach(function (s) { max = Math.max(max, s.count); });
            return '<div class="bars">' + list.map(function (s) {
                return '<div><span>' + esc(names ? (names[s.name] || s.name) : s.name) + '</span><span><i style="width:' + Math.max(3, Math.round(s.count / max * 100)) + '%"></i></span><span>' + s.count + '</span></div>';
            }).join('') + '</div>';
        }
        function tile(href, ic, color, value, label) {
            const inner = '<span class="ic ' + color + '">' + icon(ic) + '</span><span><b>' + esc(value) + '</b><span>' + esc(label) + '</span></span>';
            return href ? '<a class="tile" href="' + href + '">' + inner + '</a>' : '<div class="tile">' + inner + '</div>';
        }
        function byUpdated(list) { return list.slice().sort(function (a, b) { return a.updatedAt < b.updatedAt ? 1 : -1; }); }
        function bySlaUsed(list) { return list.slice().sort(function (a, b) { return b.slaPct - a.slaPct; }); }
        function plural(n, one, many) { return n + ' ' + (n === 1 ? one : many); }
        function emptyBox(text) { return '<div class="empty">' + esc(text) + '</div>'; }

        // ---------------- home: only what is waiting on you ----------------
        function homeView() {
            return '<div class="page-head"><div><h1 id="hHello"></h1><p id="hSub"></p></div><div id="hCta"></div></div>'
                + (state.role === 'admin' ? '<div class="tiles t4" id="hTiles"></div>' : '')
                + '<div id="hFirst"></div><div id="hSecond"></div>';
        }
        function updateHome() {
            const hour = new Date().getHours();
            byId('hHello').textContent = (hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening') + ', ' + me.name;
            const all = state.tickets;
            const attention = all.filter(needsYou);
            const attentionIds = attention.map(function (t) { return t.id; });
            const notInAttention = function (t) { return attentionIds.indexOf(t.id) < 0; };
            const m = (state.counts || {}).attention || 0;

            if (state.role === 'student') {
                const open = all.filter(function (t) { return t.stateKey !== 'CLOSED'; });
                byId('hSub').textContent = (open.length ? 'You have ' + plural(open.length, 'open incident', 'open incidents') + '. ' : 'You have no open incidents. ')
                    + (m ? (m === 1 ? '1 needs your attention.' : m + ' need your attention.') : 'Nothing is waiting on you.');
                setHtml('hCta', '<a class="btn" href="#report">' + icon('plus') + 'Report an incident</a>');
                setHtml('hFirst', attention.length ? cardHtml('Needs your attention', rowsTable(byUpdated(attention).slice(0, 5), ['num', 'desc', 'state'], ''), '<a href="#list/attention">View all</a>') : '');
                const rest = byUpdated(open.filter(notInAttention)).slice(0, 5);
                const empty = !all.length ? 'You have not reported anything yet. Use Report an incident to get started.' : attention.length ? 'No other open incidents.' : 'You have no open incidents.';
                setHtml('hSecond', cardHtml('My open incidents', rowsTable(rest, ['num', 'desc', 'state', 'updated'], emptyBox(empty)), '<a href="#list/active">View all</a>'));
            } else if (state.role === 'tech') {
                byId('hSub').textContent = 'Your queue: ' + me.skills.join(', ') + '. ' + (m ? (m === 1 ? '1 incident needs your attention.' : m + ' incidents need your attention.') : 'Nothing needs your attention right now.');
                const pct = Math.min(100, Math.round(me.workload / me.capacity * 100));
                setHtml('hCta', '<div class="workload"><small>Workload</small><b>' + me.workload + ' of ' + me.capacity + ' open</b><div class="meter"><i class="'
                    + (pct >= 90 ? 'late' : pct >= 70 ? 'risk' : '') + '" style="width:' + pct + '%"></i></div></div>');
                setHtml('hFirst', attention.length ? cardHtml('Needs your attention', rowsTable(byUpdated(attention).slice(0, 6), ['num', 'desc', 'priority', 'sla'], ''), '<a href="#list/attention">View all</a>') : '');
                const due = bySlaUsed(all.filter(function (t) { return isOpen(t) && notInAttention(t); })).slice(0, 5);
                const risky = all.filter(function (t) { return isOpen(t) && t.sla !== 'ON_TRACK'; });
                const breached = risky.some(function (t) { return t.sla === 'BREACHED'; });
                const riskLink = risky.length ? '<a href="#list/sla" style="color:var(--' + (breached ? 'red' : 'amber') + ')">' + risky.length + ' at risk or breached</a>' : '';
                setHtml('hSecond', cardHtml('Due next', rowsTable(due, ['num', 'desc', 'state', 'sla'], emptyBox('Nothing else open in your queue.')), riskLink + '<a href="#list/assigned">View all</a>'));
            } else {
                byId('hSub').textContent = 'Service desk overview for the Computer Centre and IT Services.';
                setHtml('hCta', '');
                const k = state.kpis;
                setHtml('hTiles', tile('#list/open', 'inbox', '', k.open, 'Open incidents') + tile('#list/critical', 'fire', 'red', k.critical, 'Critical and open')
                    + tile('#list/escalated', 'up', 'purple', all.filter(function (t) { return t.escalated && isOpen(t); }).length, 'Escalated') + tile('#list/breached', 'clock', 'amber', k.missed, 'SLA breached'));
                setHtml('hFirst', cardHtml('Needs your attention', rowsTable(bySlaUsed(attention).slice(0, 6), ['num', 'desc', 'priority', 'assignee', 'sla'], emptyBox('No escalated or overdue incidents.')),
                    '<a href="#list/attention">View all</a>'));
                setHtml('hSecond', '');
            }
        }

        // ---------------- incident list: one page, views as chips ----------------
        function listView() {
            if (!PRESETS[route.arg]) {
                route.arg = DEFAULT_LIST[state.role];
                history.replaceState(null, '', '#list/' + route.arg);
                cameFrom = '#list/' + route.arg;
            }
            if (activeFilterCount() > 0) { ui.filters = true; }
            const sel = function (key, label, options) {
                return '<div><label for="f-' + key + '">' + esc(label) + '</label><select id="f-' + key + '" data-filter="' + key + '"><option value="">Any</option>' + options.map(function (o) {
                    return '<option value="' + esc(o[0]) + '"' + (filters[key] === o[0] ? ' selected' : '') + '>' + esc(o[1]) + '</option>';
                }).join('') + '</select></div>';
            };
            let panel = sel('status', 'State', [['ASSIGNED', 'Assigned'], ['IN_PROGRESS', 'In progress'], ['RESOLVED', 'Resolved'], ['REOPENED', 'Reopened'], ['CLOSED', 'Closed']])
                + sel('priority', 'Priority', Object.keys(PRIORITY).map(function (p) { return [p, PRIORITY[p]]; }))
                + sel('service', 'Service', state.services.map(function (s) { return [s.key, s.name]; }))
                + sel('sla', 'SLA state', [['ON_TRACK', 'On track'], ['AT_RISK', 'At risk'], ['BREACHED', 'Breached']]);
            if (state.role === 'admin') {
                panel += sel('tech', 'Technician', state.users.filter(function (u) { return u.skills !== undefined; }).map(function (u) { return [u.id, u.name]; }));
            }
            panel += '<div><label for="f-from">Opened on or after</label><input id="f-from" type="date" data-filter="from" value="' + esc(filters.from) + '"></div>';
            return '<div class="page-head"><div><h1>' + esc(LIST_LABEL[state.role]) + '</h1></div></div><div class="chips" id="lChips"></div>'
                + '<div class="card"><div class="toolbar"><input data-filter="q" placeholder="Search this list by number or words" value="' + esc(filters.q) + '" aria-label="Search this list">'
                + '<button class="ghost' + (ui.filters ? ' on' : '') + '" id="lFilterBtn" data-ui="filters" aria-controls="lFilters">' + icon('sliders') + 'Filters<span id="lFilterN"></span></button>'
                + '<button class="link" id="lClear" data-ui="clear">Clear filters</button><span class="hint" id="lCount" style="margin-left:auto"></span></div>'
                + '<div class="filter-panel' + (ui.filters ? '' : ' hidden') + '" id="lFilters">' + panel + '</div><div id="lTable"></div></div>';
        }
        function updateList() {
            const role = state.role;
            const p = PRESETS[route.arg] || PRESETS[DEFAULT_LIST[role]];
            const keys = CHIPS[role].slice();
            if (keys.indexOf(route.arg) < 0 && PRESETS[route.arg]) { keys.push(route.arg); }
            const c = (state.counts || {}).attention || 0;
            setHtml('lChips', keys.map(function (k) {
                return '<a class="chip' + (k === route.arg ? ' on' : '') + '" href="#list/' + k + '" title="' + esc(PRESETS[k].desc) + '">' + esc(PRESETS[k].chip)
                    + (k === 'attention' && c ? '<span class="n">' + c + '</span>' : '') + '</a>';
            }).join(''));
            const n = activeFilterCount();
            byId('lFilterN').textContent = n ? ' (' + n + ')' : '';
            show('lClear', n > 0 || !!filters.q);
            const rows = byUpdated(state.tickets.filter(p.filter || function () { return true; }));
            byId('lCount').textContent = plural(rows.length, 'incident', 'incidents');
            const emptyHtml = '<div class="empty">' + esc(p.empty) + (n > 0 || filters.q ? ' No incidents match your filters. <button class="link" data-ui="clear">Clear filters</button>' : '') + '</div>';
            setHtml('lTable', rowsTable(rows, LIST_COLS[role], emptyHtml));
        }

        // ---------------- incident record: one column ----------------
        function backLabel() {
            if (cameFrom.indexOf('#home') === 0) { return 'Home'; }
            const k = cameFrom.indexOf('#list/') === 0 ? cameFrom.substring(6) : DEFAULT_LIST[state.role];
            return esc(LIST_LABEL[state.role]) + ' &middot; ' + esc((PRESETS[k] || PRESETS[DEFAULT_LIST[state.role]]).chip);
        }
        function ticketView() {
            const href = cameFrom || '#list/' + DEFAULT_LIST[state.role];
            return '<div class="narrow"><a class="back" id="rBack" href="' + esc(href) + '">&larr; Back to ' + backLabel() + '</a>'
                + '<div class="card rec"><div id="rHead"><div class="empty">Loading incident...</div></div><div id="rStatus"></div><div id="rMeta"></div>'
                + '<div id="rDetailsBox" class="hidden"><div class="fields" id="rFields"></div></div><div id="rSuggest"></div>'
                + '<div id="rManageBox" class="drawer hidden"><div id="rManage"></div></div></div>'
                + '<div class="card"><div class="card-head"><h2>Activity</h2><div id="rStreamToggle"></div></div>'
                + '<div id="rComposerWrap" class="hidden"><div class="tabs" id="rTabs"></div><div class="composer" id="rComposer">'
                + '<label for="cText" id="cLabel" style="margin-top:0">Message</label><textarea id="cText" placeholder="Type your message"></textarea>'
                + '<div class="foot"><span class="hint" id="rCompHint"></span><button data-send="1" id="rSendBtn">Send</button></div></div></div>'
                + '<ul class="stream" id="rStream"></ul></div></div>';
        }

        // The banner appears only when the viewer must act (or for an alert); otherwise one quiet line.
        function statusHtml(d) {
            const t = d.ticket;
            const staff = d.mode === 'staff';
            const banner = function (cls, title, text) {
                return '<div class="banner ' + cls + '">' + icon(cls === 'alert' ? 'alert' : 'info') + '<div><b>' + esc(title) + '</b><p>' + esc(text) + '</p></div></div>';
            };
            switch (t.needsAction) {
                case "Reply to CCIT's question":
                    return banner('', t.needsAction, 'The technician asked: "' + t.question + '" Answer in the reply box below.');
                case 'Confirm the fix':
                    return banner('', t.needsAction, 'The technician marked this as resolved' + (t.resolution ? ': "' + t.resolution + '"' : '') + ". Check that it works, then choose Yes, it's fixed or Still broken.");
                case 'Rate how it was handled':
                    return banner('', t.needsAction, 'This incident is closed. A rating from 1 to 5 helps CCIT improve.');
                case 'Pick up this ticket':
                case 'Reopened: pick it up again':
                    return banner('', t.needsAction, 'Picking it up tells the student someone is working on it and stops the response timer. ' + (t.response || ''));
                case 'Deadline missed':
                case 'Escalated to you':
                    return banner('alert', t.needsAction, (t.reason || 'This incident is past its resolution deadline').replace(/[.]$/, '') + '. Use Manage to reassign it or change its priority.');
            }
            let title = t.status;
            let text = staff ? '' : 'You will get a notification when something changes.';
            if (staff && t.stateKey === 'WAITING') { title = "Waiting for the student's reply"; text = 'You will be notified when they reply.'; }
            else if (staff && t.stateKey === 'IN_PROGRESS') { title = 'In progress'; text = 'Add work notes as you investigate, then resolve it.'; }
            else if (staff && t.stateKey === 'RESOLVED') { title = 'Resolved'; text = 'Waiting for the student to confirm the fix.'; }
            else if (t.stateKey === 'CLOSED') { title = 'Closed'; text = t.feedback ? 'Rated ' + t.feedback + '.' : ''; }
            return '<p class="status-line">' + icon('info') + '<span><b>' + esc(title) + '.</b> ' + esc(text) + '</span></p>';
        }

        function headerButtons(d) {
            const t = d.ticket;
            const staff = d.mode === 'staff';
            const typed = ['note', 'ask', 'comment', 'reply'];
            const list = t.actions.filter(function (a) { return a.style !== 'warn' && typed.indexOf(a.key) < 0; }).map(function (a) {
                let label = a.label;
                let cls = a.style === 'primary' ? '' : a.style;
                let title = '';
                if (!staff && a.key === 'close') { label = "Yes, it's fixed"; cls = ''; }
                if (!staff && a.key === 'reopen') { label = 'Still broken'; cls = 'ghost'; }
                if (a.key === 'feedback') { label = 'Rate the fix'; cls = ''; }
                if (a.key === 'start') { label = 'Pick up'; cls = ''; }
                if (a.key === 'resolve') { label = 'Resolve'; cls = ''; }
                if (a.style === 'locked') { title = ' title="Only the student can close a resolved incident"'; }
                return { cls: cls, html: '<button class="' + esc(cls) + '" data-id="' + esc(t.id) + '" data-action="' + esc(a.key) + '"' + title + '>' + esc(label) + '</button>' };
            });
            if (staff && manageHtml(d) !== '') {
                list.push({ cls: 'ghost', html: '<button class="ghost' + (ui.manage ? ' on' : '') + '" data-ui="manage" aria-expanded="' + ui.manage + '">' + icon('sliders') + 'Manage</button>' });
            }
            return list;
        }

        function updateTicket() {
            const d = ticket;
            if (!d || !byId('rHead')) { return; }
            const t = d.ticket;
            const staff = d.mode === 'staff';
            const buttons = headerButtons(d);
            const hasPrimary = buttons.some(function (b) { return b.cls === ''; });
            setHtml('rHead', '<div class="rec-head"><div><div class="sub">' + esc(t.id) + ' &middot; opened ' + esc(t.ago) + '</div><h1>' + esc(t.title) + '</h1>'
                + '<div class="labels">' + priorityPill(t.priority) + stateBadge(t) + (t.escalated && isOpen(t) ? '<span class="tag red" style="margin:0">Escalated</span>' : '') + slaChip(t) + '</div></div>'
                + '<div class="rec-actions">' + buttons.map(function (b) { return b.html; }).join('') + '</div></div>');
            setHtml('rStatus', statusHtml(d));

            // one quiet line of key facts; the rest is behind More details
            const parts = staff ? ['Caller ' + t.reporter, t.service, t.location] : [t.service, t.location, 'Assigned to ' + t.assignee];
            if (state.role === 'admin') { parts.push('Assigned to ' + t.assignee); }
            let meta = '<div class="meta">' + parts.filter(Boolean).map(esc).join(' &middot; ')
                + ' &middot; <button class="link small" data-ui="details">' + (ui.details ? 'Hide details' : 'More details') + '</button></div>';
            if (t.related.length) {
                meta += '<div class="meta related">' + icon('link') + ' Possibly the same problem as ' + t.related.map(function (r) {
                    return state.role === 'admin' ? '<a href="#ticket/' + esc(r) + '">' + esc(r) + '</a>' : esc(r);
                }).join(', ') + '</div>';
            }
            setHtml('rMeta', meta);

            show('rDetailsBox', ui.details);
            if (ui.details) {
                const urgency = { 1: 'Low: it can wait', 2: 'Medium: stopping work', 3: 'High: exam or deadline at risk' }[t.urgency] || t.urgency;
                const people = t.people >= 1000 ? 'The whole campus' : t.people >= 200 ? 'A whole hostel or building' : t.people >= 20 ? 'A floor or lab' : t.people > 1 ? 'A few people' : 'One person';
                const cls = t.sla === 'BREACHED' ? 'late' : t.sla === 'AT_RISK' ? 'risk' : '';
                const target = t.timeLeft === 'Done'
                    ? 'Completed' + (t.sla === 'BREACHED' ? ' (deadline was missed)' : '')
                    : '<span class="sla"><span class="txt ' + cls + '">' + esc(t.timeLeft) + '</span><span class="meter" style="display:block;height:8px"><i class="' + cls + '" style="width:' + Math.min(100, Math.round(t.slaPct)) + '%"></i></span></span>';
                const f = function (k, v) { return '<div class="field"><span>' + esc(k) + '</span><div>' + v + '</div></div>'; };
                setHtml('rFields', f('Opened', esc(t.created)) + f('Category', esc(t.category)) + f('Urgency', esc(urgency)) + f('Who is affected', esc(people))
                    + f('Response', esc(t.response || '-')) + f('Resolution target', target) + f('Last update', esc(t.updated)) + (t.feedback ? f('Student rating', esc(t.feedback)) : ''));
            }

            const canResolve = t.actions.some(function (a) { return a.key === 'resolve'; });
            let suggest = '';
            if (staff && isOpen(t) && t.suggestions.length) {
                const sg = t.suggestions[0];
                suggest = '<div class="callout">' + icon('check') + '<div><b>Suggested fix</b> <span class="hint">from ' + esc(sg.id) + ', ' + sg.match + '% match</span><p class="clamp">' + esc(sg.text) + '</p></div>'
                    + (canResolve ? '<button class="ghost small" data-id="' + esc(t.id) + '" data-action="resolve" data-prefill="' + esc(sg.text) + '">Use this fix</button>' : '') + '</div>';
            }
            setHtml('rSuggest', suggest);

            const manage = manageHtml(d);
            show('rManageBox', ui.manage && manage !== '');
            setHtml('rManage', manage);

            // activity: the conversation by default, every status update on request
            const CONV = ['CREATED', 'STAFF_MESSAGE', 'REPORTER_MESSAGE', 'RESOLUTION', 'NOTE', 'FEEDBACK', 'ESCALATION'];
            const MSG = ['STAFF_MESSAGE', 'REPORTER_MESSAGE', 'RESOLUTION', 'NOTE'];
            const items = d.thread.slice().reverse();
            const msgCount = items.filter(function (e) { return MSG.indexOf(e.kind) >= 0; }).length;
            setHtml('rStreamToggle', '<div class="toggle"><button data-ui="conv" class="' + (ui.stream === 'conv' ? 'on' : '') + '">Conversation (' + msgCount + ')</button>'
                + '<button data-ui="all" class="' + (ui.stream === 'all' ? 'on' : '') + '">All activity</button></div>');
            const shown = ui.stream === 'all' ? items : items.filter(function (e) { return CONV.indexOf(e.kind) >= 0; });
            const hidden = items.length - shown.length;
            setHtml('rStream', shown.map(entryHtml).join('')
                + (hidden > 0 ? '<li class="entry event more"><span>' + plural(hidden, 'status update', 'status updates') + ' hidden.</span> <button class="link small" data-ui="all">Show all activity</button></li>' : ''));

            show('rComposerWrap', d.canMessage);
            if (d.canMessage) { updateComposer(hasPrimary); }
        }

        function entryHtml(e) {
            const who = e.mine ? 'You' : (e.actor || 'System');
            const key = e.time + '|' + e.actor + '|' + e.text;
            if (e.isNew) { newOnVisit[key] = true; }
            const fresh = newOnVisit[key] ? '<span class="tag">New</span>' : '';
            if (e.kind === 'STAFF_MESSAGE' || e.kind === 'REPORTER_MESSAGE' || e.kind === 'RESOLUTION' || e.kind === 'NOTE') {
                const cls = e.kind === 'RESOLUTION' ? 'fix' : e.kind === 'NOTE' ? 'note' : '';
                const kind = e.kind === 'RESOLUTION' ? '<span class="kind fix">Resolution</span>' : e.kind === 'NOTE' ? '<span class="kind note">Work note, internal</span>' : '';
                return '<li class="entry ' + cls + '"><span class="avatar' + (e.kind === 'REPORTER_MESSAGE' ? ' alt' : '') + '">' + esc(initials(who === 'You' ? me.name : who)) + '</span>'
                    + '<div class="bubble"><div class="who"><b>' + esc(who) + '</b> ' + esc(e.time) + kind + fresh + '</div><div class="body">' + esc(e.text) + '</div></div></li>';
            }
            const sentence = (e.actor ? who + ' ' + lowerFirst(e.text) : e.text).replace(/[.]$/, '');
            return '<li class="entry event"><span class="dot">' + icon(e.kind === 'ESCALATION' || e.kind === 'SLA' ? 'alert' : 'dot') + '</span><span>' + esc(sentence) + '</span><span class="sub" style="margin-left:auto">' + esc(e.time) + '</span></li>';
        }

        function updateComposer(hasPrimary) {
            const staff = ticket && ticket.mode === 'staff';
            const t = ticket.ticket;
            if (!staff) { composeMode = 'message'; }
            if (staff && !isOpen(t)) { composeMode = 'note'; }
            show('rTabs', staff);
            if (staff) {
                setHtml('rTabs', (isOpen(t) ? [['message', 'Message to student'], ['note', 'Work note (internal)']] : [['note', 'Work note (internal)']]).map(function (x) {
                    return '<button data-mode="' + x[0] + '" class="' + (x[0] === composeMode ? 'on' : '') + '">' + esc(x[1]) + '</button>';
                }).join(''));
            }
            const note = composeMode === 'note';
            byId('rComposer').classList.toggle('note', note);
            byId('rComposer').classList.toggle('ask', !staff && ticket.waiting);
            byId('cLabel').textContent = note ? 'Work note (only CCIT staff see this)' : staff ? 'Message to ' + t.reporter : (ticket.waiting ? 'Your reply to ' + t.assignee : 'Add information for ' + t.assignee);
            byId('cText').placeholder = note ? 'Type your note' : 'Type your message';
            byId('rCompHint').textContent = note ? 'Only CCIT staff see work notes.' : staff ? 'The student is notified, and the incident waits for their reply.' : t.assignee + ' is notified when you send.';
            byId('rSendBtn').textContent = note ? 'Add work note' : staff ? 'Send message' : (ticket.waiting ? 'Send reply' : 'Send');
            byId('rSendBtn').className = hasPrimary ? 'ghost' : '';
        }

        async function sendMessage() {
            const text = byId('cText').value;
            if (!text.trim()) { toast('Write something first.', true); byId('cText').focus(); return; }
            const staff = ticket.mode === 'staff';
            const action = composeMode === 'note' ? 'note' : staff ? 'ask' : 'message';
            const r = await post('/api/action', { id: route.arg, action: action, text: text });
            if (r && r.ok) { byId('cText').value = ''; byId('cText').blur(); }
        }

        const PROMPTS = {
            resolve: ['Resolve incident', 'Describe what was done. The student sees this and is asked to confirm the fix.', { label: 'Resolution notes', ok: 'Resolve' }],
            reopen: ['Still broken', 'Tell the technician what is still wrong. The incident goes back to the same technician.', { label: 'What is still not working? (optional)', ok: 'Reopen incident', optional: true }],
            feedback: ['Rate the fix', 'How well was your problem handled?', { label: 'Comment (optional)', ok: 'Send rating', rating: true, optional: true }]
        };
        async function act(id, action, prefill) {
            const data = { id: id, action: action };
            const p = PROMPTS[action];
            if (p) {
                const opts = Object.assign({}, p[2]);
                let hint = p[1];
                const t = ticket && ticket.ticket.id === id ? ticket.ticket : null;
                if (action === 'resolve' && (prefill || (t && t.suggestions.length))) {
                    opts.value = prefill || t.suggestions[0].text;
                    hint += ' Filled in from a similar incident resolved earlier; edit as needed.';
                }
                const answer = await form(p[0] + ' (' + id + ')', hint, opts);
                if (!answer) { return; }
                data.text = answer.text;
                data.rating = answer.rating;
            }
            post('/api/action', data);
        }

        function manageHtml(d) {
            const t = d.ticket;
            const row = function (label, id, options, what, btn) {
                return '<div class="mrow"><label for="' + id + '">' + esc(label) + '</label><select id="' + id + '">' + options + '</select><button class="ghost small" data-manage="' + what + '">' + btn + '</button></div>';
            };
            let html = '';
            if (d.canChangePriority) {
                html += row('Priority', 'mPriority', Object.keys(PRIORITY).map(function (p) { return '<option value="' + p + '"' + (p === d.priorityKey ? ' selected' : '') + '>' + PRIORITY[p] + '</option>'; }).join(''), 'priority', 'Change');
            }
            if (d.canReassign) {
                html += row('Assigned to', 'mTech', state.users.filter(function (u) { return u.skills !== undefined && u.active; }).map(function (u) {
                    return '<option value="' + esc(u.id) + '"' + (u.id === d.assigneeId ? ' selected' : '') + '>' + esc(u.name) + ' (' + u.workload + '/' + u.capacity + ')</option>';
                }).join(''), 'reassign', 'Move');
                html += row('Service', 'mService', state.services.map(function (x) {
                    return '<option value="' + esc(x.key) + '"' + (x.key === d.serviceKey ? ' selected' : '') + '>' + esc(x.name) + '</option>';
                }).join(''), 'service', 'Move');
            }
            const warn = t.actions.filter(function (a) { return a.style === 'warn'; });
            if (warn.length) {
                html += '<div class="mwarn">' + warn.map(function (a) { return '<button class="warn small" data-id="' + esc(t.id) + '" data-action="' + esc(a.key) + '">' + esc(a.label) + '</button>'; }).join('')
                    + '<span class="hint">Lifecycle rules block closing an incident that is not resolved.</span></div>';
            }
            return html;
        }
        async function manage(what) {
            const id = route.arg;
            if (what === 'priority') {
                const p = byId('mPriority').value;
                if (p === ticket.priorityKey) { toast('That is already the priority.', true); return; }
                const answer = await form('Change priority to ' + PRIORITY[p], 'The SLA deadline is recalculated from when the incident was opened. The student and technician are notified.',
                    { label: 'Reason for the change', ok: 'Change priority' });
                if (answer) { post('/api/action', { id: id, action: 'priority', priority: p, text: answer.text }); }
            } else if (what === 'reassign') {
                post('/api/action', { id: id, action: 'reassign', to: byId('mTech').value });
            } else {
                post('/api/action', { id: id, action: 'service', service: byId('mService').value });
            }
        }

        // ---------------- report an incident: pick a service, then describe it ----------------
        let report = { service: '', urgency: '2', people: '40' };
        function reportView() {
            report.service = route.arg || report.service || '';
            ui.svcOpen = false;
            const choice = function (group, value, title, sub) {
                return '<button class="choice' + (report[group] === value ? ' on' : '') + '" data-choice="' + group + '|' + value + '"><b>' + esc(title) + '</b><small>' + esc(sub) + '</small></button>';
            };
            return '<div class="page-head"><div><h1>Report an incident</h1><p>Pick the service, describe the problem and submit. It goes straight to the right technician.</p></div></div>'
                + '<div class="card"><div class="card-head"><h2>1. Which service has the problem?</h2></div><div class="card-body"><div id="catalog"></div></div></div>'
                + '<div class="card hidden" id="rpStep2"><div class="card-head"><h2>2. Describe the problem</h2></div><div class="card-body" style="max-width:760px">'
                + '<label for="rpLocation" style="margin-top:0">Where is it? <span class="help">Building, floor or room, so the technician can find it</span></label>'
                + '<input id="rpLocation" placeholder="Hostel block D, 2nd floor">'
                + '<label for="rpText">What is happening?</label><textarea id="rpText" placeholder="Wi-Fi keeps dropping every few minutes since this morning"></textarea>'
                + '<label>How urgent is it?</label><div class="choices">'
                + choice('urgency', '1', 'It can wait', 'Annoying, but I can work') + choice('urgency', '2', 'Stopping my work', 'I cannot do what I need to') + choice('urgency', '3', 'Exam or deadline at risk', 'Needs fixing now')
                + '</div><label>Who is affected?</label><div class="choices">'
                + choice('people', '1', 'Just me', 'One person') + choice('people', '40', 'My floor or lab', 'Around 40 people') + choice('people', '300', 'My whole hostel', 'Hundreds of people') + choice('people', '1500', 'The whole campus', 'Everyone')
                + '</div><div style="margin-top:20px"><button id="rpSubmit" onclick="submitReport()">' + icon('plus') + 'Submit incident</button></div></div></div>';
        }
        function svcCard(s, tag) {
            return '<' + tag + ' class="svc' + (report.service === s.key ? ' on' : '') + '"' + (tag === 'button' ? ' data-svc="' + esc(s.key) + '"' : '') + '><span class="ic">' + icon(SERVICE_ICON[s.key] || 'grid') + '</span>'
                + '<span><b>' + esc(s.name) + '</b><small>' + esc(SERVICE_HINT[s.key] || s.categoryName + ' issues') + '</small></span></' + tag + '>';
        }
        function updateReport() {
            const picked = state.services.filter(function (s) { return s.key === report.service; })[0];
            if (!picked || ui.svcOpen) {
                setHtml('catalog', '<div class="catalog">' + state.services.map(function (s) { return svcCard(s, 'button'); }).join('') + '</div>'
                    + (picked ? '' : '<p class="hint" style="margin:12px 0 0">Pick a service to continue.</p>'));
            } else {
                setHtml('catalog', '<div class="picked">' + svcCard(picked, 'div') + '<button class="link" data-ui="svc">Change service</button></div>');
            }
            show('rpStep2', !!picked);
        }
        function pickService(key) {
            report.service = key;
            ui.svcOpen = false;
            byId('catalog').dataset.sig = '';
            updateReport();
            byId('rpLocation').focus();
        }
        function pickChoice(el) {
            const p = el.dataset.choice.split('|');
            report[p[0]] = p[1];
            el.parentNode.querySelectorAll('.choice').forEach(function (b) { b.classList.toggle('on', b === el); });
        }
        async function submitReport() {
            if (!report.service) { toast('Choose the service that has the problem first.', true); return; }
            const btn = byId('rpSubmit');
            btn.disabled = true;
            const r = await post('/api/report', { service: report.service, location: byId('rpLocation').value, text: byId('rpText').value, urgency: report.urgency, people: report.people });
            btn.disabled = false;
            const m = r && r.ok ? r.message.match(/INC-[0-9]+/) : null;
            if (m) { report = { service: '', urgency: '2', people: '40' }; go('#ticket/' + m[0]); }
        }

        // ---------------- reports: overview, workload, audit log ----------------
        function adminHead(title, group, hint) {
            return '<div class="page-head"><div><h1>' + esc(title) + '</h1></div></div>' + pageTabs(group) + '<p class="hint page-hint">' + esc(hint) + '</p>';
        }
        function reportsView() {
            return adminHead('Reports', 'reports', 'Incident volumes and service health.') + '<div class="tiles t3" id="rpTiles"></div>'
                + '<div class="grid-eq">' + card('Incidents by priority', 'rpPri', ' class="card-body"') + card('Incidents by service', 'rpSvc', ' class="card-body"') + '</div>';
        }
        function updateReports() {
            const k = state.kpis;
            setHtml('rpTiles', tile('#list/all', 'list', '', k.total, 'Total incidents') + tile('#list/open', 'inbox', '', k.open, 'Open')
                + tile('#list/closed', 'check', 'green', k.fixed, 'Resolved or closed') + tile('#list/breached', 'alert', 'red', k.missed, 'SLA breached')
                + tile('', 'clock', 'green', k.avgFix, 'Average time to fix') + tile('', 'star', 'amber', k.rating, 'Average rating'));
            setHtml('rpPri', bars(state.byPriority, PRIORITY));
            setHtml('rpSvc', bars(state.byService));
        }
        function workloadView() {
            return adminHead('Reports', 'reports', "Open incidents against each technician's capacity.") + card('Technician workload', 'wkTable');
        }
        function updateWorkload() {
            setHtml('wkTable', '<div class="table-wrap"><table><thead><tr><th>Technician</th><th>Skills</th><th>Open</th><th>Load</th></tr></thead><tbody>'
                + state.users.filter(function (u) { return u.skills !== undefined; }).map(function (u) {
                    const pct = Math.min(100, Math.round(u.workload / u.capacity * 100));
                    return '<tr class="' + (u.active ? '' : 'off') + '"><td><b>' + esc(u.name) + '</b>' + (u.active ? '' : ' (disabled)') + '</td><td class="sub">' + esc(u.skills) + '</td><td>' + u.workload + ' / ' + u.capacity
                        + '</td><td style="min-width:140px"><div class="meter"><i class="' + (pct >= 90 ? 'late' : pct >= 70 ? 'risk' : '') + '" style="width:' + pct + '%"></i></div></td></tr>';
                }).join('') + '</tbody></table></div>');
        }
        function auditView() {
            return adminHead('Reports', 'reports', 'The latest 80 recorded actions. Open an incident and choose All activity for its full history.') + card('Recorded actions', 'auTable');
        }
        function updateAudit() {
            setHtml('auTable', '<div class="table-wrap"><table><thead><tr><th>Time</th><th>Incident</th><th>By</th><th>Action</th><th>State change</th></tr></thead><tbody>' + state.audit.map(function (a) {
                return '<tr><td class="sub" style="white-space:nowrap">' + esc(a.time) + '</td><td class="num"><a href="#ticket/' + esc(a.id) + '">' + esc(a.id) + '</a></td><td>' + esc(a.actor) + '</td>'
                    + '<td>' + esc(a.action) + '</td><td class="sub">' + (a.to ? esc((a.from || 'Created') + ' to ' + a.to) : '') + '</td></tr>';
            }).join('') + '</tbody></table></div>');
        }

        // ---------------- settings: users, services, SLA ----------------
        function peopleView() {
            ui.addTech = false;
            return adminHead('Settings', 'settings', "A disabled account cannot sign in. A disabled technician's open incidents are handed to other technicians.")
                + cardHtml('Accounts', '<div id="ppAdd" class="card-body add-form hidden"><div class="row-2">'
                    + '<div><label for="tName" style="margin-top:0">Name</label><input id="tName" placeholder="Hostel technician (Block E)"></div>'
                    + '<div><label for="tEmail" style="margin-top:0">Email</label><input id="tEmail" placeholder="hostel-e@campus.example"></div>'
                    + '<div><label for="tUser">Username</label><input id="tUser"></div><div><label for="tPass">Password</label><input id="tPass" type="password"></div></div>'
                    + '<label>Skills <span class="help">Incidents in these categories are matched to this technician</span></label><div class="checks" id="tSkills"></div>'
                    + '<div style="display:flex;gap:8px;margin-top:16px"><button onclick="addTech()">Add technician</button><button class="ghost" data-ui="addtech">Cancel</button></div></div>'
                    + '<div id="ppUsers"></div>', '<button class="ghost small" data-ui="addtech">' + icon('plus') + 'Add technician</button>');
        }
        function updatePeople() {
            setHtml('ppUsers', '<div class="table-wrap"><table><thead><tr><th>Name</th><th>Role</th><th>Skills</th><th>Open incidents</th><th>Status</th><th></th></tr></thead><tbody>'
                + state.users.map(function (u) {
                    const btn = u.admin ? '' : '<button class="' + (u.active ? 'warn' : 'ghost') + ' small" data-toggle="' + esc(u.id) + '" data-active="' + (!u.active) + '">' + (u.active ? 'Disable' : 'Enable') + '</button>';
                    return '<tr class="' + (u.active ? '' : 'off') + '"><td><div style="display:flex;gap:10px;align-items:center"><span class="avatar' + (u.skills === undefined ? ' alt' : '') + '">' + esc(initials(u.name)) + '</span>'
                        + '<span><b>' + esc(u.name) + '</b><div class="sub">' + esc(u.email) + ' &middot; ' + esc(u.username) + '</div></span></div></td>'
                        + '<td>' + esc(u.role) + '</td><td class="sub">' + esc(u.skills || '') + '</td><td>' + (u.capacity ? u.workload + ' of ' + u.capacity : '') + '</td>'
                        + '<td>' + (u.active ? '<span class="state RESOLVED">Active</span>' : '<span class="state CLOSED">Disabled</span>') + '</td><td>' + btn + '</td></tr>';
                }).join('') + '</tbody></table></div>');
            setHtml('tSkills', state.categories.map(function (c) { return '<label><input type="checkbox" value="' + esc(c) + '">' + esc(c) + '</label>'; }).join(''));
        }
        function addTech() {
            const skills = Array.from(document.querySelectorAll('#tSkills input:checked')).map(function (c) { return c.value; });
            post('/api/admin', { op: 'addTech', name: byId('tName').value, email: byId('tEmail').value, username: byId('tUser').value, password: byId('tPass').value, skills: skills.join(',') })
                .then(function (r) {
                    if (r && r.ok) {
                        ['tName', 'tEmail', 'tUser', 'tPass'].forEach(function (i) { byId(i).value = ''; });
                        document.querySelectorAll('#tSkills input').forEach(function (c) { c.checked = false; });
                        ui.addTech = false;
                        show('ppAdd', false);
                    }
                });
        }

        function servicesView() {
            ui.addSvc = false;
            return adminHead('Settings', 'settings', 'Criticality (1 to 5) is part of the priority score: criticality times urgency times people affected.')
                + cardHtml('IT services', '<div id="svAdd" class="card-body add-form hidden"><div class="add-row">'
                    + '<div><label for="sName">Name</label><input id="sName" placeholder="Hostel laundry kiosk"></div>'
                    + '<div><label for="sCat">Category</label><select id="sCat"></select></div>'
                    + '<div><label for="sCrit">Criticality</label><select id="sCrit"><option>1</option><option>2</option><option selected>3</option><option>4</option><option>5</option></select></div>'
                    + '<div style="display:flex;gap:8px"><button onclick="addService()">Add service</button><button class="ghost" data-ui="addsvc">Cancel</button></div></div></div>'
                    + '<div id="svTable"></div>', '<button class="ghost small" data-ui="addsvc">' + icon('plus') + 'Add service</button>')
                + cardHtml('Incident categories', '<div class="card-body"><div id="svCats" style="display:flex;flex-wrap:wrap;gap:6px;margin-bottom:12px"></div>'
                    + '<div style="display:flex;gap:8px;max-width:460px"><input id="cName" placeholder="New category" aria-label="New category"><button class="ghost" onclick="addCategory()">Add category</button></div></div>');
        }
        function updateServices() {
            setHtml('svTable', '<div class="table-wrap"><table><thead><tr><th>Service</th><th>Category</th><th>Criticality</th><th>Incidents</th></tr></thead><tbody>' + state.services.map(function (x) {
                const opts = [1, 2, 3, 4, 5].map(function (n) { return '<option' + (n === x.criticality ? ' selected' : '') + '>' + n + '</option>'; }).join('');
                return '<tr><td><div style="display:flex;gap:10px;align-items:center"><span class="tile" style="padding:0;border:0;box-shadow:none"><span class="ic">' + icon(SERVICE_ICON[x.key] || 'grid') + '</span></span><b>' + esc(x.name) + '</b></div></td>'
                    + '<td>' + esc(x.categoryName) + '</td><td><select data-crit="' + esc(x.key) + '" aria-label="Criticality of ' + esc(x.name) + '">' + opts + '</select></td><td>' + x.count + '</td></tr>';
            }).join('') + '</tbody></table></div>');
            setHtml('svCats', state.categories.map(function (c) { return '<span class="state ASSIGNED">' + esc(c) + '</span>'; }).join(''));
            const sc = byId('sCat');
            if (sc && sc.options.length !== state.categories.length) {
                sc.innerHTML = state.categories.map(function (c) { return '<option value="' + esc(c) + '">' + esc(c) + '</option>'; }).join('');
            }
        }
        function addService() {
            post('/api/admin', { op: 'addService', name: byId('sName').value, category: byId('sCat').value, criticality: byId('sCrit').value })
                .then(function (r) { if (r && r.ok) { byId('sName').value = ''; ui.addSvc = false; show('svAdd', false); } });
        }
        function addCategory() {
            post('/api/admin', { op: 'addCategory', name: byId('cName').value }).then(function (r) { if (r && r.ok) { byId('cName').value = ''; } });
        }

        function slaView() {
            return adminHead('Settings', 'settings', 'Response is the time to pick an incident up; resolution is the time to fix it.')
                + '<div class="grid-eq">' + cardHtml('SLA policy', '<div id="slTable"></div><div class="card-body hint" style="border-top:1px solid var(--rule)">At risk at 75% of the resolution time. Changes apply to new incidents only.</div>')
                + card('Assignment rule', 'slStrategy', ' class="card-body"') + '</div>';
        }
        function updateSla() {
            setHtml('slTable', '<div class="table-wrap"><table><thead><tr><th>Priority</th><th>Response (min)</th><th>Resolution (min)</th><th></th></tr></thead><tbody>' + state.sla.map(function (r) {
                return '<tr><td>' + priorityPill(r.priority) + '</td><td><input id="resp-' + r.priority + '" type="number" min="1" value="' + r.response + '" aria-label="Response minutes"></td>'
                    + '<td><input id="reso-' + r.priority + '" type="number" min="1" value="' + r.resolution + '" aria-label="Resolution minutes"></td><td><button class="ghost small" data-sla="' + r.priority + '">Save</button></td></tr>';
            }).join('') + '</tbody></table></div>');
            const s = state.strategy;
            setHtml('slStrategy', '<div class="choices" style="grid-template-columns:1fr">'
                + '<button class="choice' + (s === 'Match by skill' ? ' on' : '') + '" data-strategy="skill"><b>Match by skill</b><small>The least busy technician whose skills include the incident’s category.</small></button>'
                + '<button class="choice' + (s === 'Take turns' ? ' on' : '') + '" data-strategy="turns"><b>Take turns</b><small>Round robin through the available technicians.</small></button></div>');
        }

        // ======================================================================
        // Start
        // ======================================================================
        route = parseRoute();
        if (route.page === 'home' || route.page === 'list') { cameFrom = '#' + route.page + (route.arg ? '/' + route.arg : ''); }
        setInterval(function () { if (me) { load(); } }, 2500);
        load(true);
    </script>
</body>
</html>
""";
    }
}
