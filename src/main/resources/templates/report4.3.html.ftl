<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>FACEPALM // NEURAL-LINK SECURITY AUDIT</title>
    <link href="https://fonts.googleapis.com/css2?family=Orbitron:wght@400;700&family=Plus+Jakarta+Sans:wght@300;400;600;800&family=JetBrains+Mono&display=swap" rel="stylesheet">
    <style>
        :root {
            --glass: rgba(255, 255, 255, 0.03);
            --glass-border: rgba(255, 255, 255, 0.1);
            --neon-primary: #00f2ff;   /* Cyber Blue */
            --neon-critical: #ff0055;  /* Neon Red */
            --neon-warning: #ffaa00;   /* Gold */
            --bg-deep: #0a0b10;
            --text-glow: 0 0 10px rgba(0, 242, 255, 0.5);
        }

        body {
            background: radial-gradient(circle at top right, #1a1c2c, var(--bg-deep));
            color: #e0e0e0;
            font-family: 'Plus Jakarta Sans', sans-serif;
            margin: 0; padding: 40px;
            min-height: 100vh;
        }

        .container { max-width: 1400px; margin: auto; }

        /* --- 2030 Cyber Header --- */
        header {
            display: flex; justify-content: space-between; align-items: flex-end;
            border-bottom: 1px solid var(--glass-border);
            padding-bottom: 20px; margin-bottom: 50px;
        }

        .brand h1 {
            font-family: 'Orbitron', sans-serif; font-size: 3rem; margin: 0;
            background: linear-gradient(to right, #fff, var(--neon-primary));
            -webkit-background-clip: text; -webkit-text-fill-color: transparent;
            filter: drop-shadow(var(--text-glow));
        }

        .system-status { font-family: 'JetBrains Mono'; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 2px; color: var(--neon-primary); }

        /* --- Metrics Grid --- */
        .summary-grid {
            display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 60px;
        }

        .glass-card {
            background: var(--glass);
            backdrop-filter: blur(12px);
            border: 1px solid var(--glass-border);
            border-radius: 20px;
            padding: 30px;
            position: relative; overflow: hidden;
        }

        .glass-card::before {
            content: ''; position: absolute; top: 0; left: 0; width: 4px; height: 100%;
            background: var(--neon-primary);
        }

        .glass-card.critical::before { background: var(--neon-critical); }
        .glass-card h4 { margin: 0; text-transform: uppercase; font-size: 0.7rem; letter-spacing: 3px; opacity: 0.6; }
        .glass-card .value { font-size: 3.5rem; font-weight: 800; margin: 10px 0; font-family: 'Orbitron'; }

        /* --- Leak Entries --- */
        .leak-entry { margin-bottom: 40px; }

        .leak-main {
            display: grid; grid-template-columns: 1fr 350px; gap: 2px;
            background: var(--glass-border); border-radius: 24px; overflow: hidden;
            border: 1px solid var(--glass-border);
        }

        .leak-info { background: var(--bg-deep); padding: 40px; }
        .leak-score-panel { background: rgba(0,0,0,0.4); backdrop-filter: blur(20px); padding: 40px; display: flex; flex-direction: column; justify-content: center; gap: 30px; }

        .rule-tag { font-family: 'JetBrains Mono'; color: var(--neon-primary); font-size: 0.9rem; text-transform: uppercase; margin-bottom: 10px; display: block; }
        .secret-display { font-family: 'JetBrains Mono'; font-size: 1.4rem; background: rgba(255,255,255,0.05); padding: 15px; border-radius: 12px; display: block; margin: 20px 0; border: 1px dashed var(--glass-border); }

        /* --- The Triad Scoreboard --- */
        .score-circle {
            position: relative; width: 120px; height: 120px; border-radius: 50%;
            border: 4px solid var(--glass-border); display: flex; align-items: center; justify-content: center;
            margin: auto;
        }
        .score-circle.total { border-color: var(--neon-primary); box-shadow: 0 0 20px rgba(0, 242, 255, 0.2); width: 160px; height: 160px; }
        .score-circle.total .num { font-size: 3rem; color: #fff; }
        .score-circle .label { position: absolute; bottom: -25px; font-size: 0.6rem; text-transform: uppercase; letter-spacing: 2px; opacity: 0.7; }
        .num { font-family: 'Orbitron'; font-weight: 700; font-size: 1.5rem; }

        .sub-scores { display: flex; justify-content: space-around; width: 100%; }

        /* --- Evidence Area --- */
        .occurrence { margin-top: 30px; padding: 20px; background: rgba(255,255,255,0.02); border-radius: 15px; }
        .file-link { color: var(--neon-primary); text-decoration: none; font-family: 'JetBrains Mono'; font-size: 0.8rem; }
        .code-block {
            background: #000; padding: 20px; border-radius: 10px; margin-top: 15px;
            font-family: 'JetBrains Mono'; font-size: 0.85rem; color: #aab; border-left: 2px solid var(--neon-primary);
        }

        .audit-trail { margin-top: 30px; font-size: 0.8rem; opacity: 0.5; font-style: italic; }
        .audit-trail ul { list-style: '→ '; padding-left: 20px; }

        @keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }
        .live-dot { height: 8px; width: 8px; background: var(--neon-critical); border-radius: 50%; display: inline-block; margin-right: 10px; animation: pulse 2s infinite; }
    </style>
</head>
<body>
<div class="container">
    <header>
        <div class="brand">
            <div class="system-status"><span class="live-dot"></span>System.Scan_Complete</div>
            <h1>FACEPALM v${metadata.scannerVersion}</h1>
        </div>
        <div class="meta-info">
            T-STAMP: ${metadata.timestamp}<br>
            U-PATH: ${metadata.rootPath}
        </div>
    </header>

    <section class="summary-grid">
        <div class="glass-card critical">
            <h4>Threats Detected</h4>
            <div class="value">${summary.criticalCount}</div>
        </div>
        <div class="glass-card">
            <h4>Anomalies Found</h4>
            <div class="value">${summary.warningCount}</div>
        </div>
        <div class="glass-card">
            <h4>Data Points</h4>
            <div class="value">${summary.totalOccurrences}</div>
        </div>
        <div class="glass-card">
            <h4>Nodes Scanned</h4>
            <div class="value">${summary.filesScanned}</div>
        </div>
    </section>

    <#list leaks as leak>
        <div class="leak-entry">
            <div class="leak-main">
                <div class="leak-info">
                    <span class="rule-tag">// ${ruleDictionary[leak.primaryRuleId].id}</span>
                    <h2 style="font-size: 2rem; margin: 10px 0;">${ruleDictionary[leak.primaryRuleId].name}</h2>

                    <span class="secret-display">${leak.maskedSecret}</span>

                    <div style="margin-top: 40px;">
                        <span class="system-status" style="color: #fff">Remediation Protocol</span>
                        <p style="opacity: 0.8; line-height: 1.8;">${ruleDictionary[leak.primaryRuleId].remediation}</p>
                    </div>

                    <div class="evidence-box">
                        <span class="system-status" style="color: #fff">Physical Evidence</span>
                        <#list leak.occurrences as occ>
                            <div class="occurrence">
                                <a href="#" class="file-link">${occ.relativePath} : LINE ${occ.lineNumber}</a>
                                <div class="code-block">${occ.snippet}</div>
                            </div>
                        </#list>
                    </div>

                    <div class="audit-trail">
                        <strong>Logic Trail Execution:</strong>
                        <ul>
                            <#list leak.scoreHistory as log><li>${log}</li></#list>
                        </ul>
                    </div>
                </div>

                <div class="leak-score-panel">
                    <div class="score-circle total">
                        <div class="num">${leak.aggregateScore?string("0")}</div>
                        <div class="label">Total Score</div>
                    </div>

                    <div class="sub-scores">
                        <div class="score-circle">
                            <div class="num" style="color: var(--neon-critical)">${leak.totalRisk}</div>
                            <div class="label">Risk</div>
                        </div>
                        <div class="score-circle">
                            <div class="num" style="color: var(--neon-primary)">${leak.totalConfidence}</div>
                            <div class="label">Conf</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </#list>
</div>
</body>
</html>
