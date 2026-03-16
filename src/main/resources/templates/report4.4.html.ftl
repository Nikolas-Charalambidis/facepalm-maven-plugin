<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Facepalm // Security Analysis Report</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #f9fafb;
            --white: #ffffff;
            --primary: #111827;
            --accent: #2563eb;
            --border: #e5e7eb;
            --text-main: #1f2937;
            --text-muted: #6b7280;

            /* Status Colors */
            --critical: #dc2626;
            --warning: #f59e0b;
            --success: #10b981;
        }

        body {
            background-color: var(--bg);
            color: var(--text-main);
            font-family: 'Inter', sans-serif;
            margin: 0; padding: 40px 20px;
            line-height: 1.5;
        }

        .container { max-width: 1100px; margin: 0 auto; }

        /* --- Header --- */
        .page-header {
            display: flex; justify-content: space-between; align-items: flex-end;
            margin-bottom: 40px; border-bottom: 2px solid var(--border);
            padding-bottom: 24px;
        }
        .page-header h1 { margin: 0; font-size: 1.8rem; font-weight: 800; color: var(--primary); }
        .meta-group { text-align: right; font-size: 0.85rem; color: var(--text-muted); }
        .meta-group code { color: var(--primary); font-weight: 600; }

        /* --- Stats Grid --- */
        .stats-grid {
            display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 16px; margin-bottom: 48px;
        }
        .stat-card {
            background: var(--white); border: 1px solid var(--border);
            padding: 24px; border-radius: 12px;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }
        .stat-card .label { font-size: 0.75rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); }
        .stat-card .value { font-size: 2rem; font-weight: 700; color: var(--primary); margin-top: 4px; }
        .stat-card.danger { border-top: 4px solid var(--critical); }

        /* --- Leak Card --- */
        .leak-card {
            background: var(--white); border: 1px solid var(--border);
            border-radius: 16px; margin-bottom: 32px; overflow: hidden;
            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
        }

        /* Top Bar: Scores & Title */
        .leak-header {
            padding: 24px; border-bottom: 1px solid var(--border);
            background: #fafafa; display: flex; justify-content: space-between; align-items: center;
        }
        .leak-title-group h2 { margin: 0; font-size: 1.25rem; font-weight: 700; color: var(--primary); }
        .masked-val {
            display: inline-block; margin-top: 6px; font-family: 'JetBrains Mono';
            font-size: 0.9rem; color: var(--accent); background: #eff6ff;
            padding: 2px 8px; border-radius: 4px;
        }

        /* The Scoreboard */
        .scoreboard { display: flex; gap: 12px; }
        .score-pill {
            display: flex; flex-direction: column; align-items: center;
            padding: 8px 16px; border-radius: 8px; border: 1px solid var(--border);
            background: var(--white); min-width: 80px;
        }
        .score-pill.total { background: var(--primary); border-color: var(--primary); color: var(--white); }
        .score-pill .s-val { font-size: 1.1rem; font-weight: 700; }
        .score-pill .s-label { font-size: 0.65rem; text-transform: uppercase; font-weight: 600; opacity: 0.8; }

        .leak-content { padding: 24px; }

        /* Remediation Section */
        .remediation {
            background: #fefce8; border: 1px solid #fef08a;
            padding: 16px; border-radius: 8px; margin-bottom: 24px;
            font-size: 0.95rem; color: #854d0e;
        }

        /* Occurrence Sub-cards */
        .evidence-title { font-size: 0.85rem; font-weight: 700; text-transform: uppercase; color: var(--text-muted); margin-bottom: 12px; display: block; }
        .occ-card {
            border: 1px solid var(--border); border-radius: 8px;
            padding: 16px; margin-bottom: 12px; background: #fdfdfd;
        }
        .occ-meta { font-family: 'JetBrains Mono'; font-size: 0.85rem; color: var(--accent); margin-bottom: 10px; display: block; }
        .code-snippet {
            background: #1e293b; color: #e2e8f0; padding: 16px;
            border-radius: 6px; font-family: 'JetBrains Mono'; font-size: 0.85rem;
            margin: 0; overflow-x: auto;
        }

        /* Footer / Audit */
        .audit-trail {
            margin-top: 24px; padding-top: 16px; border-top: 1px solid var(--border);
            font-size: 0.8rem; color: var(--text-muted);
        }
        .audit-trail ul { margin: 8px 0 0 0; padding-left: 18px; }
    </style>
</head>
<body>
<div class="container">
    <header class="page-header">
        <div>
            <h1>Facepalm Security</h1>
            <p style="margin: 4px 0 0; color: var(--text-muted);">Secret Detection & Risk Analysis</p>
        </div>
        <div class="meta-group">
            SCAN_ID: <code>${metadata.timestamp?replace(":", "")?replace("-", "")}</code><br>
            ROOT: <code>${metadata.rootPath}</code><br>
            ENGINE_V: <code>${metadata.scannerVersion}</code>
        </div>
    </header>

    <section class="stats-grid">
        <div class="stat-card danger">
            <div class="label">Critical Threats</div>
            <div class="value">${summary.criticalCount}</div>
        </div>
        <div class="stat-card">
            <div class="label">Warnings</div>
            <div class="value">${summary.warningCount}</div>
        </div>
        <div class="stat-card">
            <div class="label">Total Occurrences</div>
            <div class="value">${summary.totalOccurrences}</div>
        </div>
        <div class="stat-card">
            <div class="label">Files Analyzed</div>
            <div class="value">${summary.filesScanned}</div>
        </div>
    </section>

    <#list leaks as leak>
        <div class="leak-card">
            <div class="leak-header">
                <div class="leak-title-group">
                    <h2>${ruleDictionary[leak.primaryRuleId].name}</h2>
                    <span class="masked-val">${leak.maskedSecret}</span>
                </div>

                <div class="scoreboard">
                    <div class="score-pill">
                        <span class="s-val" style="color: var(--critical)">${leak.totalRisk}</span>
                        <span class="s-label">Risk</span>
                    </div>
                    <div class="score-pill">
                        <span class="s-val" style="color: var(--accent)">${leak.totalConfidence}</span>
                        <span class="s-label">Conf</span>
                    </div>
                    <div class="score-pill total">
                        <span class="s-val">${leak.aggregateScore?string("0")}</span>
                        <span class="s-label">Total Score</span>
                    </div>
                </div>
            </div>

            <div class="leak-content">
                <div class="remediation">
                    <strong>Remediation:</strong> ${ruleDictionary[leak.primaryRuleId].remediation}
                </div>

                <span class="evidence-title">Found in ${leak.occurrences?size} locations</span>
                <#list leak.occurrences as occ>
                    <div class="occ-card">
                        <span class="occ-meta">${occ.relativePath} : Line ${occ.lineNumber}</span>
                        <pre class="code-snippet"><code>${occ.snippet}</code></pre>
                    </div>
                </#list>

                <div class="audit-trail">
                    <strong>Engine Decision Logic:</strong>
                    <ul>
                        <#list leak.scoreHistory as log><li>${log}</li></#list>
                    </ul>
                </div>
            </div>
        </div>
    </#list>
</div>
</body>
</html>
