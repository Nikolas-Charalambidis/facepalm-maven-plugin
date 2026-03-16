<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Facepalm // Security Audit</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #f3f4f6;
            --panel: #ffffff;
            --text-primary: #111827;
            --text-secondary: #6b7280;
            --accent: #2563eb;
            --danger: #dc2626;      /* Red */
            --warning: #ca8a04;     /* Dark Yellow */
            --info: #0891b2;        /* Cyan/Blue */
            --border: #e5e7eb;
            --sidebar-width: 320px;
        }

        * { box-sizing: border-box; }
        html { scroll-behavior: smooth; }

        body {
            background: var(--bg); color: var(--text-primary);
            font-family: 'Inter', sans-serif; margin: 0; display: flex; min-height: 100vh;
        }

        /* --- STICKY SIDEBAR --- */
        nav {
            width: var(--sidebar-width); height: 100vh; position: fixed;
            left: 0; top: 0; background: var(--panel); border-right: 1px solid var(--border);
            padding: 24px; display: flex; flex-direction: column; z-index: 1000;
        }

        .nav-brand { font-size: 1.2rem; font-weight: 800; color: var(--text-primary); margin-bottom: 20px; }

        /* QUICK JUMP BAR */
        .jump-bar {
            display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; margin-bottom: 24px;
        }
        .jump-btn {
            text-align: center; padding: 8px; border-radius: 6px; font-size: 0.7rem; font-weight: 700;
            text-decoration: none; text-transform: uppercase; border: 1px solid var(--border);
        }
        .j-crit { background: #fee2e2; color: var(--danger); }
        .j-warn { background: #fef9c3; color: var(--warning); }
        .j-info { background: #ecfeff; color: var(--info); }

        .nav-scroll-area { flex: 1; overflow-y: auto; padding-right: 5px; }
        .nav-scroll-area::-webkit-scrollbar { width: 4px; }
        .nav-scroll-area::-webkit-scrollbar-thumb { background: var(--border); border-radius: 10px; }

        .nav-item {
            display: block; padding: 12px; margin-bottom: 8px; text-decoration: none;
            border-radius: 8px; transition: all 0.2s; border: 1px solid transparent;
        }
        .nav-item:hover { transform: translateX(4px); }

        /* Nav Item Category Colors */
        .ni-crit { background: #fff1f1; border-color: #fecaca; }
        .ni-warn { background: #fffbeb; border-color: #fef08a; }
        .ni-info { background: #f0f9ff; border-color: #bae6fd; }

        .ni-label { font-size: 0.75rem; font-weight: 700; display: block; margin-bottom: 4px; }
        .ni-meta { display: flex; justify-content: space-between; align-items: center; }
        .ni-rule { font-size: 0.8rem; font-weight: 500; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
        .ni-score { font-weight: 800; font-size: 0.9rem; }

        /* --- MAIN CONTENT --- */
        main {
            margin-left: var(--sidebar-width); flex: 1; padding: 40px 60px;
            max-width: calc(100vw - var(--sidebar-width));
        }

        .report-header { margin-bottom: 40px; }
        .summary-banner { display: flex; gap: 16px; margin-bottom: 48px; }
        .summary-tile {
            flex: 1; background: var(--panel); padding: 20px; border-radius: 12px;
            border-bottom: 4px solid var(--border);
        }
        .summary-tile.t-crit { border-color: var(--danger); }
        .summary-tile.t-warn { border-color: var(--warning); }
        .summary-tile.t-info { border-color: var(--info); }

        .finding-stack { display: flex; flex-direction: column; gap: 48px; }

        .finding-card {
            background: var(--panel); border: 1px solid var(--border);
            border-radius: 16px; display: grid; grid-template-columns: 1fr 280px;
            overflow: hidden; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1);
        }

        .card-info { padding: 40px; min-width: 0; }
        .card-metrics { background: #fafafa; padding: 40px; display: flex; flex-direction: column; justify-content: center; align-items: center; border-left: 1px solid var(--border); }

        .secret-box {
            font-family: 'JetBrains Mono'; background: #0f172a; color: #f8fafc;
            padding: 20px; margin: 24px 0; font-size: 0.9rem; border-radius: 8px;
            word-break: break-all;
        }

        .total-score-box .num { font-size: 5rem; font-weight: 800; display: block; line-height: 1; }
        .total-score-box .lab { font-weight: 700; text-transform: uppercase; font-size: 0.7rem; color: var(--text-secondary); margin-top: 10px; display: block; }

        .sub-metrics {
            width: 100%; display: grid; grid-template-columns: 1fr 1fr;
            margin-top: 32px; padding-top: 24px; border-top: 1px solid var(--border);
        }
        .metric-unit { text-align: center; }
        .metric-unit .num { font-size: 1.4rem; font-weight: 700; display: block; }
        .metric-unit .lab { font-size: 0.6rem; font-weight: 700; text-transform: uppercase; color: var(--text-secondary); }

        .code-wrap {
            background: #1e293b; color: #f1f5f9; padding: 16px; font-family: 'JetBrains Mono';
            font-size: 0.8rem; margin-top: 8px; border-radius: 8px; overflow-x: auto;
            white-space: pre-wrap; word-break: break-all;
        }

        @media (max-width: 1100px) {
            .finding-card { grid-template-columns: 1fr; }
            .card-metrics { border-left: none; border-top: 1px solid var(--border); }
        }
    </style>
</head>
<body>

<nav>
    <div class="nav-brand">Facepalm.io</div>

    <div class="jump-bar">
        <a href="#section-crit" class="jump-btn j-crit">Critical</a>
        <a href="#section-warn" class="jump-btn j-warn">Warning</a>
        <a href="#section-info" class="jump-btn j-info">Info</a>
    </div>

    <div class="nav-scroll-area">
        <#list leaks as leak>
            <#assign cat = (leak.aggregateScore > 80)?string("crit", (leak.aggregateScore > 40)?string("warn", "info"))>
            <a href="#leak-${leak?index}" class="nav-item ni-${cat}">
                <span class="ni-label" style="color: var(--${cat})">${cat?upper_case}</span>
                <div class="ni-meta">
                    <span class="ni-rule">${ruleDictionary[leak.primaryRuleId].name}</span>
                    <span class="ni-score" style="color: var(--${cat})">${leak.aggregateScore?string("0")}</span>
                </div>
            </a>
        </#list>
    </div>
</nav>

<main>
    <header class="report-header">
        <h1 style="letter-spacing: -0.04em;">Security Analysis Results</h1>
        <p style="color: var(--text-secondary); font-weight: 500;">${metadata.timestamp} &bull; Target: ${metadata.rootPath}</p>
    </header>

    <section class="summary-banner">
        <div id="section-crit" class="summary-tile t-crit">
            <div style="font-size: 0.75rem; font-weight: 800; color: var(--danger);">CRITICAL</div>
            <div style="font-size: 2.5rem; font-weight: 800;">${summary.criticalCount}</div>
        </div>
        <div id="section-warn" class="summary-tile t-warn">
            <div style="font-size: 0.75rem; font-weight: 800; color: var(--warning);">WARNING</div>
            <div style="font-size: 2.5rem; font-weight: 800;">${summary.warningCount}</div>
        </div>
        <div id="section-info" class="summary-tile t-info">
            <div style="font-size: 0.75rem; font-weight: 800; color: var(--info);">INFO</div>
            <#assign infoCount = leaks?filter(l -> l.aggregateScore <= 40)?size>
            <div style="font-size: 2.5rem; font-weight: 800;">${infoCount}</div>
        </div>
        <div class="summary-tile">
            <div style="font-size: 0.75rem; font-weight: 800; color: var(--text-secondary);">TOTAL HITS</div>
            <div style="font-size: 2.5rem; font-weight: 800;">${summary.totalOccurrences}</div>
        </div>
    </section>

    <div class="finding-stack">
        <#list leaks as leak>
        <#-- Dynamic logic for color assignment -->
            <#assign sevColor = (leak.aggregateScore > 80)?string("var(--danger)", (leak.aggregateScore > 40)?string("var(--warning)", "var(--info)"))>

            <article class="finding-card" id="leak-${leak?index}">
                <div class="card-info">
                    <span style="font-family: 'JetBrains Mono'; font-size: 0.75rem; color: var(--text-secondary); font-weight: 600;">RULE_REF: ${leak.primaryRuleId}</span>
                    <h2 style="font-size: 1.75rem; margin: 10px 0 20px 0; font-weight: 800; color: var(--text-primary);">${ruleDictionary[leak.primaryRuleId].name}</h2>

                    <div style="background: #f8fafc; border: 1px solid var(--border); padding: 16px; border-radius: 8px; font-size: 0.9rem; color: var(--text-secondary);">
                        <strong style="color: var(--text-primary)">Remediation:</strong> ${ruleDictionary[leak.primaryRuleId].remediation}
                    </div>

                    <div class="secret-box">
                        ${leak.maskedSecret}
                    </div>

                    <div style="margin-top: 32px;">
                        <span style="font-weight: 700; font-size: 0.85rem; color: var(--text-secondary); text-transform: uppercase;">Evidence Log</span>
                        <#list leak.occurrences as occ>
                            <div style="margin-top: 16px;">
                                <span style="font-family: 'JetBrains Mono'; font-size: 0.8rem; color: var(--accent); font-weight: 600;">${occ.relativePath} : L${occ.lineNumber}</span>
                                <div class="code-wrap"><code>${occ.snippet}</code></div>
                            </div>
                        </#list>
                    </div>
                </div>

                <div class="card-metrics">
                    <div class="total-score-box">
                        <span class="num" style="color: ${sevColor}">${leak.aggregateScore?string("0")}</span>
                        <span class="lab">Total Score</span>
                    </div>

                    <div class="sub-metrics">
                        <div class="metric-unit">
                            <span class="num" style="color: var(--danger)">${leak.totalRisk}</span>
                            <span class="lab">Risk</span>
                        </div>
                        <div class="metric-unit">
                            <span class="num" style="color: var(--accent)">${leak.totalConfidence}</span>
                            <span class="lab">Confidence</span>
                        </div>
                    </div>
                </div>
            </article>
        </#list>
    </div>
</main>
</body>
</html>
