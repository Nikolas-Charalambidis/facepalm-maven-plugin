<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Facepalm</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #f3f4f6;
            --panel: #ffffff;
            --text-primary: #111827;
            --text-secondary: #4b5563;
            --accent: #2563eb;
            --danger: #ef4444;
            --warning: #f59e0b;
            --info: #3b82f6;
            --border: #e5e7eb;
            --sidebar-width: 320px;
        }

        * { box-sizing: border-box; }
        html { scroll-behavior: smooth; }

        body {
            background: var(--bg); color: var(--text-primary);
            font-family: 'Inter', sans-serif; margin: 0; display: flex; min-height: 100vh;
            line-height: 1.5;
        }

        /* --- STICKY SIDEBAR --- */
        nav {
            width: var(--sidebar-width); height: 100vh; position: fixed;
            left: 0; top: 0; background: var(--panel); border-right: 1px solid var(--border);
            padding: 32px 20px; display: flex; flex-direction: column; z-index: 1000;
        }

        .nav-brand {
            font-size: 1.5rem; font-weight: 800; color: var(--text-primary);
            margin-bottom: 32px; letter-spacing: -0.025em;
            display: flex; align-items: center; gap: 8px;
        }
        .nav-brand::before {
            content: "🤦"; font-size: 1.75rem;
        }

        .jump-bar { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; margin-bottom: 32px; }
        .jump-btn {
            text-align: center; padding: 10px 4px; border-radius: 8px; font-size: 0.7rem; font-weight: 700;
            text-decoration: none; text-transform: uppercase; border: 1px solid var(--border); cursor: pointer;
            transition: all 0.2s;
        }
        .j-crit { background: #fee2e2; color: #991b1b; border-color: #fecaca; }
        .j-warn { background: #fef3c7; color: #92400e; border-color: #fde68a; }
        .j-info { background: #dbeafe; color: #1e40af; border-color: #bfdbfe; }
        .jump-btn:hover { transform: translateY(-2px); filter: brightness(0.95); }

        .nav-scroll-area { flex: 1; overflow-y: auto; padding-right: 4px; margin: 0 -4px; }
        .nav-scroll-area::-webkit-scrollbar { width: 4px; }
        .nav-scroll-area::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 10px; }

        .nav-item {
            display: block; padding: 16px; margin-bottom: 12px; text-decoration: none;
            border-radius: 12px; transition: all 0.2s; border: 1px solid var(--border);
            background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,0.05);
        }
        .nav-item:hover { transform: translateX(4px); border-color: var(--accent); box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1); }

        .ni-crit { border-left: 4px solid var(--danger); }
        .ni-warn { border-left: 4px solid var(--warning); }
        .ni-info { border-left: 4px solid var(--info); }

        .ni-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 6px; }
        .ni-rule { font-size: 0.75rem; line-height: 1.6; color: var(--text-secondary); text-transform: uppercase; }
        .ni-score { font-family: 'JetBrains Mono'; font-weight: 800; font-size: 0.85rem; }
        .ni-secret { font-family: 'JetBrains Mono'; font-size: 0.7rem; color: var(--text-secondary); display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; opacity: 0.8; }

        /* --- MAIN CONTENT --- */
        main {
            margin-left: var(--sidebar-width); flex: 1;
            padding: 64px 80px;
            max-width: 1400px;
        }

        .report-header { margin-bottom: 48px; }
        .summary-banner { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; margin-bottom: 64px; }

        .summary-tile {
            background: var(--panel); padding: 24px; border-radius: 16px;
            border: 1px solid var(--border);
            box-shadow: 0 1px 3px rgba(0,0,0,0.1);
            transition: transform 0.2s;
        }
        .summary-tile:hover { transform: translateY(-4px); }

        .finding-stack { display: flex; flex-direction: column; gap: 48px; }

        .finding-card {
            background: var(--panel); border: 1px solid var(--border);
            border-radius: 24px; display: grid; grid-template-columns: 1fr 320px;
            overflow: hidden; box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.05), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
        }

        .card-info { padding: 48px; min-width: 0; }
        .card-metrics { background: #f9fafb; padding: 48px; display: flex; flex-direction: column; justify-content: flex-start; align-items: center; border-left: 1px solid var(--border); }

        .severity-badge {
            display: inline-flex; align-items: center; padding: 4px 12px; border-radius: 9999px;
            font-size: 0.7rem; font-weight: 800; text-transform: uppercase; letter-spacing: 0.05em;
        }
        .badge-crit { background: #fee2e2; color: #991b1b; }
        .badge-warn { background: #fef3c7; color: #92400e; }
        .badge-info { background: #dbeafe; color: #1e40af; }

        .secret-box {
            font-family: 'JetBrains Mono'; background: #111827; color: #f3f4f6;
            padding: 24px; margin: 32px 0; font-size: 1rem; border-radius: 12px;
            word-break: break-all; position: relative; border: 1px solid #374151;
            box-shadow: inset 0 2px 4px 0 rgba(0, 0, 0, 0.06);
        }

        .total-score-box { text-align: center; }
        .total-score-box .num { font-size: 6rem; font-weight: 900; display: block; line-height: 1; letter-spacing: -0.05em;}
        .total-score-box .lab { font-weight: 700; text-transform: uppercase; font-size: 0.75rem; color: var(--text-secondary); margin-top: 8px; display: block; letter-spacing: 0.1em; }

        .sub-metrics {
            width: 100%; display: grid; grid-template-columns: 1fr 1fr;
            margin-top: 48px; padding-top: 32px; border-top: 1px solid var(--border);
            gap: 16px;
        }

        .metric-item { text-align: center; }
        .metric-val { font-family: 'JetBrains Mono'; font-size: 1.75rem; font-weight: 800; display: block; }
        .metric-lab { font-size: 0.65rem; font-weight: 800; color: var(--text-secondary); text-transform: uppercase; margin-top: 4px; letter-spacing: 0.05em; }

        .remediation-box {
            background: #f8fafc; border: 1px solid var(--border); padding: 24px; border-radius: 12px;
            font-size: 1rem; line-height: 1.6; color: var(--text-secondary);
        }
        .remediation-box strong { color: var(--text-primary); display: block; margin-bottom: 4px; font-size: 0.85rem; text-transform: uppercase; letter-spacing: 0.025em; }

        .code-wrap {
            background: #1f2937; color: #f9fafb; padding: 24px; font-family: 'JetBrains Mono';
            font-size: 0.85rem; margin-top: 12px; border-radius: 12px; overflow-x: auto;
            white-space: pre-wrap; word-break: break-all; border-left: 4px solid var(--accent);
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
        }

        .logic-trail {
            margin-top: 48px; width: 100%; font-size: 0.75rem; color: var(--text-secondary);
            border-top: 1px solid var(--border); padding-top: 24px;
        }
        .logic-trail strong { color: var(--text-primary); text-transform: uppercase; letter-spacing: 0.05em; font-size: 0.7rem; }
        .logic-trail ul { margin: 12px 0 0 0; padding-left: 1.25rem; }
        .logic-trail li { margin-bottom: 6px; }

        @media (max-width: 1280px) {
            main { padding: 48px 40px; }
            .finding-card { grid-template-columns: 1fr; }
            .card-metrics { border-left: none; border-top: 1px solid var(--border); padding: 48px; }
            .sub-metrics { max-width: 400px; }
        }
    </style>
</head>
<body>

<nav>
    <div class="nav-brand">Facepalm Report</div>

    <div class="jump-bar">
        <a onclick="scrollToSection('crit')" class="jump-btn j-crit">Critical</a>
        <a onclick="scrollToSection('warn')" class="jump-btn j-warn">Warning</a>
        <a onclick="scrollToSection('info')" class="jump-btn j-info">Info</a>
    </div>

    <div class="nav-scroll-area">
        <#list leaks as leak>
            <#assign cat = (leak.aggregateScore > 80)?string("crit", (leak.aggregateScore > 40)?string("warn", "info"))>
            <a href="#leak-${leak?index}" class="nav-item ni-${cat} find-cat-${cat}">
                <div class="ni-top">
                    <span class="ni-rule" >${ruleDictionary[leak.primaryRuleId].name}</span>
                    <span class="ni-score" style="color: var(--${(cat == "crit")?string("danger", (cat == "warn")?string("warning", "info"))})">${leak.aggregateScore?string("0")}</span>
                </div>
                <span class="ni-secret">${leak.maskedSecret}</span>
            </a>
        </#list>
    </div>
</nav>

<main>
    <header class="report-header">
        <h1 style="font-size: 2.5rem; letter-spacing: -0.04em; margin: 0;">facepalm-maven-plugin:1.0.0</h1>
        <p style="color: var(--text-secondary); font-weight: 500; margin-top: 12px;">
            Generated: ${metadata.timestamp} &bull; Target: <code style="color: var(--text-primary)">${metadata.rootPath}</code>
        </p>
    </header>

    <section class="summary-banner">
        <div class="summary-tile" style="border-top: 4px solid var(--danger);">
            <div style="font-size: 0.7rem; font-weight: 800; color: var(--danger); letter-spacing: 0.1em; margin-bottom: 8px;">CRITICAL</div>
            <div style="font-size: 2.5rem; font-weight: 900;">${summary.criticalCount}</div>
        </div>
        <div class="summary-tile" style="border-top: 4px solid var(--warning);">
            <div style="font-size: 0.7rem; font-weight: 800; color: var(--warning); letter-spacing: 0.1em; margin-bottom: 8px;">WARNING</div>
            <div style="font-size: 2.5rem; font-weight: 900;">${summary.warningCount}</div>
        </div>
        <div class="summary-tile" style="border-top: 4px solid var(--info);">
            <div style="font-size: 0.7rem; font-weight: 800; color: var(--info); letter-spacing: 0.1em; margin-bottom: 8px;">INFO</div>
            <#assign infoCount = leaks?filter(l -> l.aggregateScore <= 40)?size>
            <div style="font-size: 2.5rem; font-weight: 900;">${infoCount}</div>
        </div>
        <div class="summary-tile" style="border-top: 4px solid var(--text-secondary);">
            <div style="font-size: 0.7rem; font-weight: 800; color: var(--text-secondary); letter-spacing: 0.1em; margin-bottom: 8px;">TOTAL HITS</div>
            <div style="font-size: 2.5rem; font-weight: 900;">${summary.totalOccurrences}</div>
        </div>
    </section>

    <div class="finding-stack">
        <#list leaks as leak>
            <#assign sev = (leak.aggregateScore > 80)?string("crit", (leak.aggregateScore > 40)?string("warn", "info"))>
            <#assign sevColor = (sev == "crit")?string("var(--danger)", (sev == "warn")?string("var(--warning)", "var(--info)"))>

            <article class="finding-card card-cat-${sev}" id="leak-${leak?index}">
                <div class="card-info">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;">
                        <span style="font-family: 'JetBrains Mono'; font-size: 0.75rem; color: var(--text-secondary); font-weight: 600; background: #f1f5f9; padding: 4px 10px; border-radius: 6px;">ID: ${leak.primaryRuleId}</span>
                        <span class="severity-badge badge-${sev}">${sev}</span>
                    </div>

                    <h2 style="font-size: 2.25rem; margin: 0 0 24px 0; font-weight: 800; color: var(--text-primary); letter-spacing: -0.025em;">${ruleDictionary[leak.primaryRuleId].name}</h2>

                    <div class="remediation-box">
                        <strong>Remediation Protocol</strong>
                        ${ruleDictionary[leak.primaryRuleId].remediation}
                    </div>

                    <#--                    <div class="secret-box">-->
                    <#--                        <div style="font-size: 0.65rem; text-transform: uppercase; opacity: 0.6; margin-bottom: 12px; font-weight: 700; letter-spacing: 0.05em;">Masked Evidence</div>-->
                    <#--                        ${leak.maskedSecret}-->
                    <#--                    </div>-->
                    <div style="margin-top: 48px;">
                        <span style="font-weight: 800; font-size: 0.8rem; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.1em;">Source Verification</span>
                        <#list leak.occurrences as occ>
                            <div style="margin-top: 24px;">
                                <div style="display: flex; align-items: center; gap: 8px;">
                                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" style="color: var(--accent)"><path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/></svg>
                                    <span style="font-family: 'JetBrains Mono'; font-size: 0.85rem; color: var(--accent); font-weight: 700;">${occ.relativePath} : L${occ.lineNumber}</span>
                                </div>
                                <div class="code-wrap"><code>${occ.snippet}</code></div>
                            </div>
                        </#list>
                    </div>
                </div>

                <div class="card-metrics">
                    <div class="total-score-box">
                        <span class="num" style="color: ${sevColor}">${leak.aggregateScore?string("0")}</span>
                        <span class="lab">Final Score</span>
                    </div>

                    <div class="sub-metrics">
                        <div class="metric-item">
                            <span class="metric-val" style="color: var(--danger);">${leak.totalRisk}</span>
                            <div class="metric-lab">Risk</div>
                        </div>
                        <div class="metric-item">
                            <span class="metric-val" style="color: var(--accent);">${leak.totalConfidence}</span>
                            <div class="metric-lab">Confidence</div>
                        </div>
                    </div>

                    <div class="logic-trail">
                        <strong>Logic Trail</strong>
                        <ul>
                            <#list leak.scoreHistory as log><li>${log}</li></#list>
                        </ul>
                    </div>
                </div>
            </article>
        </#list>
    </div>
</main>

<script>
    function scrollToSection(category) {
        // Find the first card with the class matching the category
        const element = document.querySelector('.card-cat-' + category);
        if (element) {
            element.scrollIntoView({ behavior: 'smooth', block: 'start' });
        } else {
            console.log("No findings found for category: " + category);
        }
    }
</script>
</body>
</html>
