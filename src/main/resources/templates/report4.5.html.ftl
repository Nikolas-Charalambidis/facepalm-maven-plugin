<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Facepalm // Industrial Security Audit</title>
    <link href="https://fonts.googleapis.com/css2?family=Orbitron:wght@700&family=Inter:wght@400;600;800&family=JetBrains+Mono&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #f8fafc;
            --panel: #ffffff;
            --text-primary: #0f172a;
            --text-secondary: #64748b;
            --accent: #2563eb;
            --danger: #ef4444;
            --border: #e2e8f0;
            --sidebar-width: 320px;
        }

        * { box-sizing: border-box; }
        html { scroll-behavior: smooth; }

        body {
            background: var(--bg);
            color: var(--text-primary);
            font-family: 'Inter', sans-serif;
            margin: 0;
            display: flex;
            min-height: 100vh;
        }

        /* --- FIXED NAVIGATION SIDEBAR --- */
        nav {
            width: var(--sidebar-width);
            height: 100vh;
            position: fixed;
            left: 0; top: 0;
            background: var(--panel);
            border-right: 2px solid var(--text-primary);
            padding: 40px 20px;
            display: flex;
            flex-direction: column;
            z-index: 1000;
        }

        .nav-brand {
            font-family: 'Orbitron', sans-serif;
            font-size: 1.4rem;
            letter-spacing: 2px;
            margin-bottom: 10px;
            color: var(--text-primary);
        }

        .nav-sub {
            font-size: 0.7rem;
            text-transform: uppercase;
            letter-spacing: 1px;
            color: var(--text-secondary);
            margin-bottom: 40px;
        }

        .nav-scroll-area {
            flex: 1;
            overflow-y: auto;
            padding-right: 10px;
        }

        /* Scrollbar Styling for Sidebar */
        .nav-scroll-area::-webkit-scrollbar { width: 4px; }
        .nav-scroll-area::-webkit-scrollbar-thumb { background: var(--border); border-radius: 10px; }

        .nav-item {
            display: block;
            padding: 15px;
            margin-bottom: 10px;
            background: var(--bg);
            border: 1px solid var(--border);
            text-decoration: none;
            color: var(--text-primary);
            border-radius: 4px;
            transition: all 0.2s;
        }

        .nav-item:hover {
            border-color: var(--accent);
            transform: translateX(5px);
        }

        .nav-item .nav-id { font-family: 'JetBrains Mono'; font-size: 0.7rem; display: block; opacity: 0.6; }
        .nav-item .nav-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 5px; }
        .nav-item .nav-score { font-family: 'Orbitron'; font-size: 1.1rem; color: var(--accent); }

        /* --- MAIN CONTENT AREA --- */
        main {
            margin-left: var(--sidebar-width);
            flex: 1;
            padding: 60px 80px;
            max-width: 1200px;
        }

        .report-header {
            margin-bottom: 60px;
            border-left: 10px solid var(--text-primary);
            padding-left: 30px;
        }

        .report-header h1 {
            font-family: 'Orbitron', sans-serif;
            font-size: 3rem;
            margin: 0;
            line-height: 1;
        }

        /* --- SUMMARY TILES --- */
        .summary-banner {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 20px;
            margin-bottom: 80px;
        }

        .summary-tile {
            background: var(--panel);
            padding: 25px;
            border: 1px solid var(--border);
            box-shadow: 4px 4px 0px var(--border);
        }

        .summary-tile .lab { font-size: 0.7rem; font-weight: 800; text-transform: uppercase; color: var(--text-secondary); }
        .summary-tile .val { font-family: 'Orbitron'; font-size: 2.2rem; margin-top: 5px; }

        /* --- FINDING CARDS (VERTICAL STACK) --- */
        .finding-stack { display: flex; flex-direction: column; gap: 100px; }

        .finding-card {
            background: var(--panel);
            border: 2px solid var(--text-primary);
            display: grid;
            grid-template-columns: 1fr 340px;
            min-height: 500px;
            box-shadow: 15px 15px 0px rgba(15, 23, 42, 0.05);
        }

        /* Left side: Info */
        .card-info { padding: 50px; border-right: 1px solid var(--border); }
        .card-rule-id { font-family: 'JetBrains Mono'; color: var(--accent); font-weight: 800; font-size: 0.9rem; }
        .card-title { font-size: 2.2rem; margin: 10px 0 30px 0; font-weight: 800; letter-spacing: -1px; }

        .secret-box {
            font-family: 'JetBrains Mono';
            background: var(--text-primary);
            color: #fff;
            padding: 20px;
            margin: 30px 0;
            font-size: 1.2rem;
            word-break: break-all;
        }

        .remediation-callout {
            background: #fffbeb;
            border: 1px solid #fef08a;
            padding: 20px;
            color: #854d0e;
            font-size: 0.95rem;
            margin-bottom: 40px;
        }

        /* Right side: Cyber Metrics */
        .card-metrics {
            background: #fcfcfc;
            padding: 50px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            text-align: center;
        }

        .total-score-big { margin-bottom: 50px; }
        .total-score-big .num { font-family: 'Orbitron'; font-size: 7rem; display: block; line-height: 1; color: var(--text-primary); }
        .total-score-big .lab { font-weight: 800; text-transform: uppercase; letter-spacing: 4px; font-size: 0.7rem; color: var(--text-secondary); }

        .sub-metrics {
            width: 100%;
            display: grid;
            grid-template-columns: 1fr 1fr;
            border-top: 1px solid var(--border);
            padding-top: 30px;
        }

        .metric-unit .num { font-family: 'Orbitron'; font-size: 1.8rem; display: block; }
        .metric-unit .lab { font-size: 0.6rem; font-weight: 700; text-transform: uppercase; color: var(--text-secondary); }

        /* Evidence & Code */
        .evidence-header { font-weight: 800; text-transform: uppercase; font-size: 0.8rem; margin-bottom: 15px; display: block; border-bottom: 1px solid var(--border); padding-bottom: 5px;}
        .occ-item { margin-bottom: 25px; }
        .occ-path { font-family: 'JetBrains Mono'; font-size: 0.85rem; color: var(--accent); font-weight: 600; }
        .code-wrap {
            background: #1e293b;
            color: #f1f5f9;
            padding: 20px;
            font-family: 'JetBrains Mono';
            font-size: 0.85rem;
            margin-top: 10px;
            overflow-x: auto;
        }

        .audit-history { font-size: 0.75rem; color: var(--text-secondary); margin-top: 40px; opacity: 0.7; }

        @media (max-width: 1100px) {
            .finding-card { grid-template-columns: 1fr; }
            .card-info { border-right: none; border-bottom: 1px solid var(--border); }
            .card-metrics { padding: 40px; }
        }
    </style>
</head>
<body>

<nav>
    <div class="nav-brand">FACEPALM</div>
    <div class="nav-sub">Security Audit Ledger</div>

    <div class="nav-scroll-area">
        <#list leaks as leak>
            <a href="#leak-${leak?index}" class="nav-item">
                <span class="nav-id">${leak.primaryRuleId}</span>
                <div class="nav-meta">
                    <span style="font-weight: 700; font-size: 0.8rem;">${leak.ruleName?default("Leak #" + leak?index)}</span>
                    <span class="nav-score">${leak.aggregateScore?string("0")}</span>
                </div>
            </a>
        </#list>
    </div>
</nav>

<main>
    <header class="report-header">
        <h1>AUDIT REPORT</h1>
        <p style="margin-top: 10px; font-weight: 600; color: var(--text-secondary);">
            TIMESTAMP: ${metadata.timestamp} &nbsp; // &nbsp; SCAN_ROOT: ${metadata.rootPath}
        </p>
    </header>

    <section class="summary-banner">
        <div class="summary-tile">
            <div class="lab">Critical</div>
            <div class="val" style="color: var(--danger)">${summary.criticalCount}</div>
        </div>
        <div class="summary-tile">
            <div class="lab">Warning</div>
            <div class="val">${summary.warningCount}</div>
        </div>
        <div class="summary-tile">
            <div class="lab">Hits</div>
            <div class="val">${summary.totalOccurrences}</div>
        </div>
        <div class="summary-tile">
            <div class="lab">Nodes Scanned</div>
            <div class="val">${summary.filesScanned}</div>
        </div>
    </section>

    <div class="finding-stack">
        <#list leaks as leak>
            <article class="finding-card" id="leak-${leak?index}">
                <div class="card-info">
                    <span class="card-rule-id">${leak.primaryRuleId}</span>
                    <h2 class="card-title">${ruleDictionary[leak.primaryRuleId].name}</h2>

                    <div class="remediation-callout">
                        <strong>REMEDIATION PROTOCOL:</strong> ${ruleDictionary[leak.primaryRuleId].remediation}
                    </div>

                    <div class="secret-box">
                        ${leak.maskedSecret}
                    </div>

                    <div style="margin-top: 50px;">
                        <span class="evidence-header">Physical Evidence</span>
                        <#list leak.occurrences as occ>
                            <div class="occ-item">
                                <span class="occ-path">${occ.relativePath} : Line ${occ.lineNumber}</span>
                                <div class="code-wrap"><code>${occ.snippet}</code></div>
                            </div>
                        </#list>
                    </div>

                    <div class="audit-history">
                        <strong>LOGIC_TRAIL:</strong>
                        <ul style="margin-top: 10px;">
                            <#list leak.scoreHistory as log><li>${log}</li></#list>
                        </ul>
                    </div>
                </div>

                <div class="card-metrics">
                    <div class="total-score-big">
                        <span class="num">${leak.aggregateScore?string("0")}</span>
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
