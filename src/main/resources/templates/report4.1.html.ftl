<!DOCTYPE html>
<html>
<head>
    <title>Facepalm Scan Report</title>
    <style>
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f8f9fa; color: #333; margin: 0; padding: 20px; }
        .container { max-width: 1100px; margin: auto; }
        .header { background: #212529; color: white; padding: 20px; border-radius: 8px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; }
        .summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin-bottom: 30px; }
        .card { background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); border-top: 4px solid #dee2e6; }
        .card.critical { border-top-color: #dc3545; }
        .card.warning { border-top-color: #ffc107; }
        .leak-box { background: white; border-radius: 8px; margin-bottom: 20px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .leak-header { padding: 15px 20px; background: #f1f3f5; border-bottom: 1px solid #dee2e6; display: flex; justify-content: space-between; align-items: center; }
        .leak-body { padding: 20px; }
        .badge { padding: 4px 12px; border-radius: 20px; font-size: 0.8em; font-weight: bold; }
        .badge-risk { background: #ffebeb; color: #dc3545; }
        .badge-conf { background: #e7f5ff; color: #1971c2; }
        .occurrence { font-family: monospace; font-size: 0.9em; background: #f8f9fa; padding: 10px; border-left: 3px solid #adb5bd; margin-top: 10px; word-break: break-all; }
        .audit-log { font-size: 0.85em; color: #6c757d; margin-top: 15px; }
    </style>
</head>
<body>
<div class="container">
    <div class="header">
        <div><h1>Facepalm Scan Report</h1><small>v${metadata.scannerVersion} | ${metadata.timestamp}</small></div>
        <div style="text-align: right"><strong>Root:</strong> ${metadata.rootPath}</div>
    </div>

    <div class="summary-cards">
        <div class="card critical"><h3>${summary.criticalCount}</h3><p>Critical Leaks</p></div>
        <div class="card warning"><h3>${summary.warningCount}</h3><p>Warnings</p></div>
        <div class="card"><h3>${summary.totalOccurrences}</h3><p>Total Hits</p></div>
        <div class="card"><h3>${summary.filesScanned}</h3><p>Files Scanned</p></div>
    </div>

    <#list leaks as leak>
        <div class="leak-box">
            <div class="leak-header">
                <div>
                    <strong>${ruleDictionary[leak.primaryRuleId].name}</strong>
                    <code style="margin-left: 15px; color: #666;">${leak.maskedSecret}</code>
                </div>
                <div>
                    <span class="badge badge-risk">Risk: ${leak.totalRisk}</span>
                    <span class="badge badge-conf">Conf: ${leak.totalConfidence}</span>
                </div>
            </div>
            <div class="leak-body">
                <p><strong>Remediation:</strong> ${ruleDictionary[leak.primaryRuleId].remediation}</p>
                <strong>Locations:</strong>
                <#list leak.occurrences as occ>
                    <div class="occurrence">
                        <strong>${occ.relativePath}:${occ.lineNumber}</strong><br/>
                        <span style="color: #495057;">${occ.snippet}</span>
                    </div>
                </#list>
                <div class="audit-log">
                    <strong>Logic Trail:</strong>
                    <ul style="margin: 5px 0;">
                        <#list leak.scoreHistory as log><li>${log}</li></#list>
                    </ul>
                </div>
            </div>
        </div>
    </#list>
</div>
</body>
</html>
