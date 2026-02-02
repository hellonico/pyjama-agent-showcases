# Email Agents (Pure EDN)

Two Pyjama agents for email automation - **no Clojure code required!** Everything is defined in EDN.

## Agents

### 1. Email Watcher Agent (`email-watcher-agent.edn`)

Monitors your inbox and summarizes new emails using the LLM.

**Features:**
- Continuously polls for unread emails
- LLM summarizes each email highlighting key points and action items  
- Runs in an endless loop

**Usage:**
```bash
cd email-agents
clojure -M:watcher
```

**Output:**
```
🤖 PYJAMA AGENTS 🤖

📧 New email received:
From: john@example.com
Subject: Meeting Tomorrow

📝 Summary: John is requesting a meeting tomorrow at 2pm to discuss 
the quarterly review. Action: Confirm availability and send calendar invite.
```

### 2. Email Sender Agent (`email-sender-agent.edn`)

Generates professional emails using LLM and sends them.

**Features:**
- LLM composes email based on your prompt
- Automatically generates subject and body
- Sends via SMTP

**Usage:**
```bash
cd email-agents

# Example: Send a thank you email
clojure -M -m pyjama.cli.agent run email-sender-agent \
  '{"to":"colleague@company.com","request":"write a thank you email for helping with the project"}' \
  -Dagents.edn=email-sender-agent.edn
```

**Or customize the alias in deps.edn and run:**
```bash
clojure -M:sender
```

## Configuration

Add email settings to `~/secrets.edn`:

```clojure
{:email
 {:smtp {:host "smtp.gmail.com"
         :port 587
         :user "your@gmail.com"
         :pass "your-app-password"
         :tls true}
  
  :imap {:host "imap.gmail.com"
         :port 993
         :user "your@gmail.com"
         :pass "your-app-password"
         :ssl true}
  
  :defaults {:from "your@gmail.com"}}}
```

**Gmail users:** Generate an [app-specific password](https://myaccount.google.com/apppasswords).

## Architecture

```
email-agents/
├── email-watcher-agent.edn    # Watches inbox, summarizes with LLM
├── email-sender-agent.edn     # Generates and sends emails via LLM
├── deps.edn                    # Dependencies only (no source code!)
└── README.md

Dependencies:
└── email-client/
    └── src/email_client/tools/
        └── registry.clj        # Tool implementations
```

## How It Works

### EDN-Based Agents

Both agents are **pure EDN** - no Clojure code needed!

```edn
{:my-agent
 {:description "What the agent does"
  :start :first-step
  
  :tools
  {:my-tool {:fn some.namespace/my-function}}
  
  :steps
  {:first-step
   {:prompt "LLM prompt here with {{variables}}"
    :routes [{:when condition :next :next-step}
             {:else :done}]}
   
   :next-step
   {:tool :my-tool
    :args {:param "{{value}}"}
    :terminal? true}}}}
```

### Automatic Loading

Pyjama automatically:
1. Loads the EDN file
2. Registers tools from `:tools` section
3. Loads secrets from `secrets.edn`
4. Executes the workflow
5. Passes data between steps
6. Calls LLM for prompt steps
7. Loops as needed

**Zero Clojure code required!**

## Use Cases

### Email Watcher
- 📬 Monitor support emails and alert on urgent issues
- 🤖 Auto-categorize incoming messages
- 📊 Track email patterns and generate reports
- 🔔 Real-time notifications for important senders

### Email Sender  
- 🎯 Draft professional responses from bullet points
- 📝 Generate follow-up emails automatically
- 🌍 Translate and send emails in different languages
- 📅 Create and send meeting requests

## Examples

### Watch Emails
```bash
clojure -M:watcher
```

Runs forever, summarizing each new email as it arrives.

### Send Thank You Email
```bash
clojure -M -m pyjama.cli.agent run email-sender-agent \
  '{"to":"mentor@company.com","request":"thank them for the career advice"}' \
  -Dagents.edn=email-sender-agent.edn
```

### Send Meeting Request
```bash
clojure -M -m pyjama.cli.agent run email-sender-agent \
  '{"to":"team@company.com","request":"schedule a standup meeting for Monday 10am"}' \
  -Dagents.edn=email-sender-agent.edn
```

## Advanced

### Customize Agents

Edit the EDN files to change behavior:

**Change polling interval (watcher):**
```edn
:watch-loop
{:tool :watch-emails
 :args {:interval-ms 10000}  ; 10 seconds instead of 5
 ...}
```

**Add custom logic (sender):**
```edn
:generate-email
{:prompt "You are a formal business email writer.
         Always use professional language and include a signature.
         
         Request: {{request}}
         ..."
 ...}
```

### Chain Agents

Combine agents for workflows:
1. Watcher detects "meeting request" email
2. Triggers Sender to generate confirmation
3. Auto-send reply

## Benefits of EDN Agents

✅ **Declarative** - Pure data, no code  
✅ **Hot-reloadable** - Edit EDN, run immediately  
✅ **Version controlled** - Track changes in git  
✅ **LLM-friendly** - Easy for AI to generate/modify  
✅ **Maintainable** - No programming required  
✅ **Composable** - Mix and match tools

## See Also

- [Email Client Library](../../../email-client/) - Tool implementations
- [Pyjama Framework](https://github.com/hellonico/pyjama) - Agent runtime
- [Other Agents](../) - More examples
