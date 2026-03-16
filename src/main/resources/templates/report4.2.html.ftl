<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Facepalm | Security Scan Report</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&family=JetBrains+Mono&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-body: #f4f7f6;
            --bg-card: #ffffff;
            --text-main: #333333;
            --text-muted: #7f8c8d;
            --border-color: #e6e9eb;
            --primary: #2c3e50; /* Dark Navy for Header */
            --accent: #3498db;  /* Blue for interactive elements */

            /* Severity Colors */
            --color-critical: #e74c3c; /* Red */
            --color-warning: #f39c12;  /* Orange */
            --color-info: #3498db;     /* Blue */
        }

        body { font-family: 'Inter', sans-serif; background: var(--bg-body); color: var(--text-main); margin: 0; padding: 0; line-height: 1.6; }
        .main-container { max-width: 1200px; margin: 0 auto; padding: 40px 20px; }

        /* --- Header: Clean, Dark Mode --- */
        .page-header { background: var(--primary); color: white; padding: 30px; border-radius: 12px; margin-bottom: 40px; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
        .brand { display: flex; align-items: center; gap: 15px; }
        .brand h1 { margin: 0; font-size: 2em; font-weight: 700; letter-spacing: -1px; }
        .brand span { font-size: 0.9em; opacity: 0.7; }
        .meta-info { text-align: right; font-size: 0.9em; opacity: 0.9; }

        /* --- Summary Section: Grid Layout --- */
        .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 20px; margin-bottom: 40px; }
        .stat-card { background: var(--bg-card); padding: 25px; border-radius: 12px; box-shadow: 0 2px 4px rgba(0,0,0,0.03); border: 1px solid var(--border-color); display: flex; flex-direction: column; justify-content: space-between; transition: transform 0.2s; }
        .stat-card:hover { transform: translateY(-3px); box-shadow: 0 4px 8px rgba(0,0,0,0.06); }
        .stat-card h3 { margin: 0; font-size: 2.5em; font-weight: 700; color: var(--primary); }
        .stat-card p { margin: 5px 0 0; color: var(--text-muted); font-size: 1.1em; }

        /* Severity Indicators on Cards */
        .stat-card.critical { border-left: 5px solid var(--color-critical); }
        .stat-card.warning { border-left: 5px solid var(--color-warning); }
        .stat-card.critical h3 { color: var(--color-critical); }
        .stat-card.warning h3 { color: var(--color-warning); }

        /* --- Leaks Section: Grouped Cards --- */
        .leak-list { display: flex; flex-direction: column; gap: 25px; }
        .leak-item { background: var(--bg-card); border-radius: 12px; box-shadow: 0 2px 10px rgba(0,0,0,0.05); border: 1px solid var(--border-color); overflow: hidden; transition: border-color 0.2s; }
        .leak-item:hover { border-color: #bdc3c7; }

        /* Leak Header: Responsive Flex */
        .leak-item-header { padding: 20px 25px; background: #fafbfc; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; gap: 15px; flex-wrap: wrap;}
        .leak-title { display: flex; flex-direction: column; gap: 5px; }
        .rule-name { font-size: 1.25em; font-weight: 600; color: var(--primary); }
        .masked-secret { font-family: 'JetBrains Mono', monospace; background: #eef2f5; padding: 4px 10px; border-radius: 6px; font-size: 0.9em; color: var(--text-main); }

        .score-badges { display: flex; gap: 10px; align-items: center; }
        .badge { padding: 6px 14px; border-radius: 20px; font-size: 0.8em; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
        .badge.severity-error { background: rgba(231, 76, 60, 0.1); color: var(--color-critical); }
        .badge.severity-warning { background: rgba(243, 156, 18, 0.1); color: var(--color-warning); }

        /* The Actual Content */
        .leak-item-body { padding: 25px; }
        .remediation-box { background: rgba(52, 152, 219, 0.05); border: 1px solid rgba(52, 152, 219, 0.2); border-radius: 8px; padding: 15px 20px; color: #2c3e50; font-size: 0.95em; margin-bottom: 25px; }

        .section-title { font-weight: 600; color: var(--primary); margin-bottom: 15px; display: block; border-bottom: 2px solid #ecf0f1; padding-bottom: 5px;}

        /* Occurrences/Snippets */
        .occurrence-list { display: flex; flex-direction: column; gap: 15px; margin-bottom: 25px;}
        .occurrence-card { background: #fdfdfe; border: 1px solid var(--border-color); border-radius: 8px; padding: 15px; transition: background 0.2s; }
        .occurrence-card:hover { background: #fbfcfc; }
        .file-path { font-family: 'JetBrains Mono', monospace; font-size: 0.9em; font-weight: 600; color: var(--accent); text-decoration: none; }
        .file-path:hover { text-decoration: underline; }
        .line-num { color: var(--text-muted); font-weight: 400; margin-left: 10px;}

        /* Code Snippet: Dark Mode Code */
        .code-snippet { font-family: 'JetBrains Mono', monospace; font-size: 0.85em; background: #2c3e50; color: #ecf0f1; padding: 15px; border-radius: 6px; margin-top: 10px; overflow-x: auto; white-space: pre-wrap; word-break: break-all;}

        /* Logic Trail/Audit Log */
        .logic-trail { font-size: 0.9em; color: var(--text-muted); background: #fcfdfe; border: 1px solid #f0f3f5; border-radius: 8px; padding: 15px 20px;}
        .logic-trail ul { margin: 5px 0 0 20px; padding: 0; }
        .logic-trail li { margin-bottom: 4px; }

        @media (max-width: 768px) {
            .page-header { flex-direction: column; align-items: flex-start; gap: 15px; }
            .meta-info { text-align: left; }
            .leak-item-header { flex-direction: column; align-items: flex-start; }
            .score-badges { align-self: flex-start; }
        }
    </style>
</head>
<body>
<div class="main-container">
    <header class="page-header">
        <div class="brand">
            <h1>Facepalm</h1>
            <span>| Security Scan Report</span>
        </div>
        <div class="meta-info">
            <strong>Version:</strong> ${metadata.scannerVersion}<br/>
            <strong>Timestamp:</strong> ${metadata.timestamp}<br/>
            <strong>Target:</strong> <code>${metadata.rootPath}</code>
        </div>
    </header>

    <section class="summary-grid">
        <div class="stat-card critical">
            <h3>${summary.criticalCount}</h3>
            <p>Critical Leaks</p>
        </div>
        <div class="stat-card warning">
            <h3>${summary.warningCount}</h3>
            <p>Warnings Identified</p>
        </div>
        <div class="stat-card">
            <h3>${summary.totalOccurrences}</h3>
            <p>Total Hits (across files)</p>
        </div>
        <div class="stat-card">
            <h3>${summary.filesScanned}</h3>
            <p>Files Analyzed</p>
        </div>
    </section>

    <section class="leak-list">
        <#list leaks as leak>
            <div class="leak-item">
                <div class="leak-item-header">
                    <div class="leak-title">
                        <span class="rule-name">${ruleDictionary[leak.primaryRuleId].name}</span>
                        <code class="masked-secret">${leak.maskedSecret}</code>
                    </div>
                    <div class="score-badges">
                        <#if (leak.aggregateScore > 80)>
                            <span class="badge severity-error">Critical</span>
                        <#else>
                            <span class="badge severity-warning">Warning</span>
                        </#if>
                        <span class="badge badge-risk" title="Raw impact score">Impact: ${leak.totalRisk}/100</span>
                        <span class="badge badge-conf" title="Detection confidence">Conf: ${leak.totalConfidence}/100</span>
                    </div>
                </div>

                <div class="leak-item-body">
                    <div class="remediation-box">
                        <strong>Action Required:</strong> ${ruleDictionary[leak.primaryRuleId].remediation}
                    </div>

                    <span class="section-title">Evidence & Locations (${leak.occurrences?size})</span>
                    <div class="occurrence-list">
                        <#list leak.occurrences as occ>
                            <div class="occurrence-card">
                                <div>
                                    <span class="file-path">${occ.relativePath}</span>
                                    <span class="line-num">Line ${occ.lineNumber}</span>
                                </div>
                                <pre class="code-snippet"><code>${occ.snippet}</code></pre>
                            </div>
                        </#list>
                    </div>

                    <div class="logic-trail">
                        <strong>Detection Logic Trail (Score: ${leak.aggregateScore?string("0.1")})</strong>
                        <ul>
                            <#list leak.scoreHistory as log><li>${log}</li></#list>
                        </ul>
                    </div>
                </div>
            </div>
        </#list>
    </section>
</div>
</body>
</html>
