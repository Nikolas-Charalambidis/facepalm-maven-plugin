<!DOCTYPE html>
<html lang="en" data-theme="light">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Security Scan Report | 2026 Edition</title>
    <style>
        :root {
            --bg-base: #f8fafc; --bg-surface: #ffffff; --text-main: #0f172a; --text-muted: #64748b; --border: #e2e8f0; --primary: #6366f1;
            --err: #ef4444; --bg-err: #fef2f2; --warn: #f59e0b; --bg-warn: #fffbeb; --info: #3b82f6; --bg-info: #eff6ff;
        }
        [data-theme="dark"] {
            --bg-base: #0f172a; --bg-surface: #1e293b; --text-main: #f8fafc; --text-muted: #94a3b8; --border: #334155;
            --bg-err: #450a0a; --bg-warn: #451a03; --bg-info: #172554;
        }
        html { scroll-behavior: smooth; font-family: system-ui, -apple-system, sans-serif; }
        body { background: var(--bg-base); color: var(--text-main); margin: 0; display: grid; grid-template-columns: 280px 1fr; min-height: 100vh; transition: background 0.3s; }

        /* Sidebar */
        .sidebar { position: sticky; top: 0; height: 100vh; background: var(--bg-surface); border-right: 1px solid var(--border); padding: 1.5rem; display: flex; flex-direction: column; gap: 1.5rem; box-sizing: border-box; }
        .nav-item { display: flex; justify-content: space-between; align-items: center; padding: 0.75rem; text-decoration: none; color: inherit; border-radius: 8px; font-weight: 500; }
        .nav-item:hover { background: var(--bg-base); }
        .badge { padding: 2px 8px; border-radius: 12px; font-size: 0.75rem; font-weight: bold; }
        .badge.error { background: var(--bg-err); color: var(--err); }
        .badge.warning { background: var(--bg-warn); color: var(--warn); }
        .badge.info { background: var(--bg-info); color: var(--info); }

        /* Main Content */
        .main { padding: 2rem 4rem; max-width: 1100px; width: 100%; box-sizing: border-box; }
        .leak-card { background: var(--bg-surface); border: 1px solid var(--border); border-radius: 12px; padding: 1.5rem; margin-bottom: 2rem; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
        .header-row { display: flex; justify-content: space-between; align-items: start; }
        .score { width: 44px; height: 44px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; border: 3px solid; }
        .score.error { border-color: var(--err); color: var(--err); }
        .score.warning { border-color: var(--warn); color: var(--warn); }
        .score.info { border-color: var(--info); color: var(--info); }

        .secret-box { font-family: 'Cascadia Code', 'Source Code Pro', monospace; background: var(--bg-base); padding: 1rem; border-radius: 6px; border: 1px solid var(--border); word-break: break-all; margin: 1rem 0; font-size: 0.9rem; }
        .occ { background: var(--bg-base); border-radius: 6px; margin-top: 0.5rem; overflow: hidden; border: 1px solid var(--border); }
        .occ-head { background: rgba(0,0,0,0.05); padding: 0.5rem 1rem; font-size: 0.8rem; display: flex; justify-content: space-between; font-weight: 600; }
        pre { margin: 0; padding: 1rem; font-size: 0.85rem; overflow-x: auto; color: var(--text-muted); }

        .theme-btn { cursor: pointer; background: var(--bg-base); border: 1px solid var(--border); border-radius: 50%; width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; }

        @media (max-width: 1024px) { body { grid-template-columns: 1fr; } .sidebar { position: relative; height: auto; border-right: none; border-bottom: 1px solid var(--border); } }
    </style>
</head>
<body>

<#-- MACRO DEFINITION -->
<#macro renderLeak leak sClass>
<#-- Safely retrieve rule info -->
    <#assign rule = (report.ruleDictionary[leak.primaryRuleId])!{ "name": "Unknown Rule", "description": "No description available." }>
    <div class="leak-card">
        <div class="header-row">
            <div>
                <h3 style="margin:0">${rule.name}</h3>
                <small style="color:var(--text-muted)">Rule ID: ${leak.primaryRuleId} | Confidence: ${leak.totalConfidence}%</small>
            </div>
            <div class="score ${sClass}">${leak.aggregateScore}</div>
        </div>

        <p style="font-size: 0.85rem; color: var(--text-muted); margin: 0.5rem 0;">${rule.description!""}</p>

        <div class="secret-box">${leak.maskedSecret}</div>

        <#if rule.remediation??>
            <div style="font-size: 0.85rem; padding: 0.75rem; background: var(--bg-info); border-left: 4px solid var(--info); margin-bottom: 1rem; border-radius: 4px;">
                <strong>Remediation:</strong> ${rule.remediation}
            </div>
        </#if>

        <#if leak.occurrences??>
            <#list leak.occurrences as occ>
                <div class="occ">
                    <div class="occ-head">
                        <span>📄 ${occ.relativePath}</span>
                        <span>Line ${occ.lineNumber}</span>
                    </div>
                    <pre><code>${occ.snippet?html}</code></pre>
                </div>
            </#list>
        </#if>
    </div>
</#macro>

<aside class="sidebar">
    <div style="display:flex; justify-content:space-between; align-items:center">
        <strong style="font-size: 1.2rem;">🛡️ Facepalm report</strong>
        <button class="theme-btn" onclick="toggleTheme()" title="Toggle Dark/Light">🌓</button>
    </div>

    <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 10px; font-size: 0.8rem; text-align: center;">
        <div style="background:var(--bg-base); padding:10px; border-radius:8px; border: 1px solid var(--border);">
            <strong style="font-size: 1.1rem;">${summary.totalLeaksFound}</strong><br><span style="color:var(--text-muted)">Unique</span>
        </div>
        <div style="background:var(--bg-base); padding:10px; border-radius:8px; border: 1px solid var(--border);">
            <strong style="font-size: 1.1rem;">${summary.totalOccurrences}</strong><br><span style="color:var(--text-muted)">Total</span>
        </div>
    </div>

    <nav style="display:flex; flex-direction:column; gap:8px">
        <a href="#section-error" class="nav-item"><span>🔴 Error</span><span class="badge error">80+</span></a>
        <a href="#section-warning" class="nav-item"><span>🟠 Warning</span><span class="badge warning">40-79</span></a>
        <a href="#section-info" class="nav-item"><span>🔵 Info</span><span class="badge info">0-39</span></a>
    </nav>

    <div style="margin-top:auto; font-size:0.7rem; color:var(--text-muted); line-height: 1.4;">
        Scanner v${metadata.scannerVersion}<br>
        Root: ${metadata.rootPath}<br>
        Ran: ${metadata.timestamp}
    </div>
</aside>

<main class="main">
    <h1 style="margin-top: 0;">facepalm-maven-plugin</h1>
    <p style="color: var(--text-muted); margin-bottom: 3rem;">Automated security analysis of potential leaked credentials.</p>

    <section id="section-error">
        <h2 style="color:var(--err); border-bottom: 2px solid var(--err); padding-bottom: 0.5rem; margin-bottom: 1.5rem;">Critical / Error (80-100)</h2>
        <#assign errCount = 0>
        <#list leaks as leak>
            <#if (leak.aggregateScore >= 80)>
                <#assign errCount++>
                <@renderLeak leak=leak sClass="error" />
            </#if>
        </#list>
        <#if errCount == 0><p style="color:var(--text-muted)">No critical leaks detected.</p></#if>
    </section>

    <section id="section-warning" style="margin-top: 4rem;">
        <h2 style="color:var(--warn); border-bottom: 2px solid var(--warn); padding-bottom: 0.5rem; margin-bottom: 1.5rem;">Warning (40-79)</h2>
        <#assign warnCount = 0>
        <#list leaks as leak>
            <#if (leak.aggregateScore >= 40 && leak.aggregateScore < 80)>
                <#assign warnCount++>
                <@renderLeak leak=leak sClass="warning" />
            </#if>
        </#list>
        <#if warnCount == 0><p style="color:var(--text-muted)">No warnings detected.</p></#if>
    </section>

    <section id="section-info" style="margin-top: 4rem;">
        <h2 style="color:var(--info); border-bottom: 2px solid var(--info); padding-bottom: 0.5rem; margin-bottom: 1.5rem;">Info (0-39)</h2>
        <#assign infoCount = 0>
        <#list leaks as leak>
            <#if (leak.aggregateScore < 40)>
                <#assign infoCount++>
                <@renderLeak leak=leak sClass="info" />
            </#if>
        </#list>
        <#if infoCount == 0><p style="color:var(--text-muted)">No low-risk secrets detected.</p></#if>
    </section>
</main>

<script>
    function toggleTheme() {
        const html = document.documentElement;
        const current = html.getAttribute('data-theme');
        const next = current === 'dark' ? 'light' : 'dark';
        html.setAttribute('data-theme', next);
        localStorage.setItem('report-theme', next);
    }

    // Initialize theme from local storage
    const savedTheme = localStorage.getItem('report-theme');
    if (savedTheme) {
        document.documentElement.setAttribute('data-theme', savedTheme);
    } else if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
        document.documentElement.setAttribute('data-theme', 'dark');
    }
</script>

</body>
</html>
