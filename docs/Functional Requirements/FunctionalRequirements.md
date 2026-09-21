# Functional Requirements

| ID | Sub-System | Description | Status | Epic |
|----|-----------|-------------|--------|------|
| FR-001 | Teams & Access | A signed-in user can create a team (name, description) and automatically becomes its owner. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| FR-002 | Teams & Access | A user sees a "My teams" page listing only the teams they belong to, with their role in each. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| FR-003 | Teams & Access | An owner can add an existing user to the team as owner, editor or viewer, change a member's role, and remove a member. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| FR-004 | Teams & Access | A team always keeps at least one owner: the last owner cannot be removed or demoted. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| FR-005 | Teams & Access | A user can belong to several teams with a different role in each. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| FR-006 | Teams & Access | Owners and editors can create, edit and archive the team's products from the team page; viewers see them read-only. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| FR-007 | Teams & Access | A `TeamAccessService` answers read / edit / owner checks for a team, a product or any node beneath it, and every service method touching team-owned data goes through it; list queries are filtered by the caller's team ids. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| FR-008 | Teams & Access | Users can sign in through a company identity provider brokered by Keycloak or a local Keycloak account; the set-up is documented for deployers. | NEW | epic_01_TEAMS_AND_SCOPED_ACCESS |
| FR-009 | Tree Editor | A signed-in user sees a "Trees" page listing the teams they belong to and can open a team's tree from it. | NEW | epic_02_TREE_EDITOR_CORE |
| FR-010 | Tree Editor | The server returns a team's whole tree (products, outcomes, nested opportunities, solutions) in a single request, ordered by `sortOrder`. | NEW | epic_02_TREE_EDITOR_CORE |
| FR-011 | Tree Editor | The tree editor renders the team's products as top-level branches with Outcome → nested Opportunity → Solution beneath each, visually distinguishing node types. | NEW | epic_02_TREE_EDITOR_CORE |
| FR-012 | Tree Editor | A user can add a product to the team's tree from the editor (name, description, vision). | NEW | epic_02_TREE_EDITOR_CORE |
| FR-013 | Tree Editor | A user can add an outcome under a product, an opportunity under an outcome or another opportunity, and a solution under an opportunity; new nodes are appended last among their siblings. | NEW | epic_02_TREE_EDITOR_CORE |
| FR-014 | Tree Editor | A user can edit a selected node's fields in a detail panel and the node updates in place when the save returns. | NEW | epic_02_TREE_EDITOR_CORE |
| FR-015 | Tree Editor | A user can set the status of an outcome, opportunity or solution (and archive a product); the status is shown on the node. | NEW | epic_02_TREE_EDITOR_CORE |
| FR-016 | Tree Editor | A user can delete a node after confirmation; deleting a node deletes its descendants. | NEW | epic_02_TREE_EDITOR_CORE |
| FR-017 | Tree Editor | A user can focus the canvas on a single product and return to the whole-team view. | NEW | epic_02_TREE_EDITOR_CORE |
| FR-018 | Tree Editor | A user can drag a node onto a valid new parent and the node moves with its whole subtree (outcome → product; opportunity → outcome or opportunity; solution → opportunity). | NEW | epic_03_TREE_REARRANGING |
| FR-019 | Tree Editor | A user can reorder a node among its siblings, and products along the top row; order is persisted via `sortOrder`. | NEW | epic_03_TREE_REARRANGING |
| FR-020 | Tree Editor | Invalid drops (wrong parent type, a node into its own subtree, another team's node) are visibly refused in the UI and rejected by the server. | NEW | epic_03_TREE_REARRANGING |
| FR-021 | Tree Editor | A user can collapse and expand any branch; collapse state is remembered per user in the browser. | NEW | epic_03_TREE_REARRANGING |
| FR-022 | Tree Editor | A user can move a node through a "Move to…" menu action as a non-drag alternative. | NEW | epic_03_TREE_REARRANGING |
| FR-023 | Tree Editor | A user can add, edit and delete assumptions under a solution with statement, category, importance (1–5) and evidence (1–5). | NEW | epic_04_ASSUMPTIONS_AND_EXPERIMENTS |
| FR-024 | Tree Editor | A user can add, edit and delete experiments under a solution with title, hypothesis, method, success criteria and dates. | NEW | epic_04_ASSUMPTIONS_AND_EXPERIMENTS |
| FR-025 | Tree Editor | A user can link an experiment to one or more assumptions of the same solution; the link is shown in the tree and in both detail panels. | NEW | epic_04_ASSUMPTIONS_AND_EXPERIMENTS |
| FR-026 | Tree Editor | A user can set an experiment's status and, when completed, its result (supported / refuted / inconclusive) and learnings. | NEW | epic_04_ASSUMPTIONS_AND_EXPERIMENTS |
| FR-027 | Tree Editor | A user can mark an assumption validated or invalidated; the assumption node shows its state and the results of its linked experiments. | NEW | epic_04_ASSUMPTIONS_AND_EXPERIMENTS |
| FR-028 | Tree Editor | The team tree payload and canvas include assumptions and experiments beneath their solution. | NEW | epic_04_ASSUMPTIONS_AND_EXPERIMENTS |
| FR-029 | Real-Time | After a tree write commits, the server publishes a change event (node type, id, action, DTO, acting user) to `/topic/teams/{teamId}/tree`; nothing is published for a rolled-back write. | NEW | epic_05_REALTIME_COLLABORATION |
| FR-030 | Real-Time | A client with a team's tree open subscribes to that team's topic and applies incoming create, update, delete and move events to its tree store so the canvas updates without a reload. | NEW | epic_05_REALTIME_COLLABORATION |
| FR-031 | Real-Time | The client applies its own edits when the REST call returns and ignores the echoed event for them. | NEW | epic_05_REALTIME_COLLABORATION |
| FR-032 | Real-Time | Topic subscriptions are authorised against `TeamAccessService` in a STOMP channel interceptor; a user cannot subscribe to a team they do not belong to, and clients cannot send to tree topics. | NEW | epic_05_REALTIME_COLLABORATION |
| FR-033 | Real-Time | When the WebSocket connection is lost the UI shows it; on reconnect the client reloads the whole tree rather than replaying events. | NEW | epic_05_REALTIME_COLLABORATION |
| FR-034 | Real-Time | A node changed by someone else is briefly highlighted with that user's name; if the node is open in the detail panel, the panel shows the new values (last write wins). | NEW | epic_05_REALTIME_COLLABORATION |
| FR-035 | Comments | A team member with edit rights can add a comment to an outcome, opportunity or solution from the node's detail panel; viewers can read comments. | NEW | epic_06_THREADED_COMMENTS |
| FR-036 | Comments | A user can reply to a comment, forming a thread shown in chronological order under its parent. | NEW | epic_06_THREADED_COMMENTS |
| FR-037 | Comments | A user can edit or delete their own comments; edited comments are marked as edited, and deleting a comment with replies leaves a "deleted" placeholder. | NEW | epic_06_THREADED_COMMENTS |
| FR-038 | Comments | Nodes with comments show a comment count badge on the canvas. | NEW | epic_06_THREADED_COMMENTS |
| FR-039 | Comments | New, edited and deleted comments are delivered live to everyone viewing the team's tree, updating open threads and badges. | NEW | epic_06_THREADED_COMMENTS |
| FR-040 | Interviews | A team member with edit rights can log an interview against one of the team's products with title, participant, date, notes, recording URL and interviewer. | NEW | epic_07_INTERVIEWS_AS_EVIDENCE |
| FR-041 | Interviews | A team member can browse the team's interviews, filter them by product and search by title or participant, and open one to read it. | NEW | epic_07_INTERVIEWS_AS_EVIDENCE |
| FR-042 | Interviews | A user can link an interview to one or more opportunities of the same team, and unlink it, from both the interview page and the opportunity detail panel. | NEW | epic_07_INTERVIEWS_AS_EVIDENCE |
| FR-043 | Interviews | An opportunity node shows the number of supporting interviews, and its detail panel lists them with links to open each. | NEW | epic_07_INTERVIEWS_AS_EVIDENCE |
| FR-044 | Interviews | A user with edit rights can edit and delete an interview; deleting removes its links but not the opportunities. | NEW | epic_07_INTERVIEWS_AS_EVIDENCE |
| FR-045 | Links | A user with edit rights can paste a URL into an opportunity's or solution's detail panel to add a link to it. | NEW | epic_08_JIRA_AND_CONFLUENCE_LINKS |
| FR-046 | Links | The app suggests the link type and name from the URL pattern: Jira issue URLs become `TICKET` named by issue key, Confluence page URLs become `DOCUMENT` named from the URL's page title, anything else `OTHER` named by host. | NEW | epic_08_JIRA_AND_CONFLUENCE_LINKS |
| FR-047 | Links | A user can change a link's name and type, remove it, and reorder a node's links. | NEW | epic_08_JIRA_AND_CONFLUENCE_LINKS |
| FR-048 | Links | Links are shown as typed chips (icon per type) in the detail panel and open in a new tab. | NEW | epic_08_JIRA_AND_CONFLUENCE_LINKS |
| FR-049 | Links | Nodes with links show a link indicator with the count on the canvas. | NEW | epic_08_JIRA_AND_CONFLUENCE_LINKS |
| FR-050 | Overview | A user with the `ROLE_OVERVIEW` authority (assigned in Keycloak) has read access to every team's tree through `TeamAccessService` without being a team member, and no write access from that authority. | NEW | epic_09_LEADERSHIP_OVERVIEW |
| FR-051 | Overview | An overview page lists all teams with their products and outcomes (status, metric, current and target value) and the number of opportunities per status under each outcome. | NEW | epic_09_LEADERSHIP_OVERVIEW |
| FR-052 | Overview | From the overview a user can open any team's tree in read-only mode, including live updates if real-time collaboration is in place. | NEW | epic_09_LEADERSHIP_OVERVIEW |
| FR-053 | Overview | Overview users see interview titles, dates and linked opportunities but never interview notes, participants or recording URLs unless they are also a member of that team. | NEW | epic_09_LEADERSHIP_OVERVIEW |
| FR-054 | MCP Server | The application exposes an MCP endpoint served by the same Spring Boot process using Spring AI's MCP server starter (WebMVC). | NEW | epic_10_MCP_SERVER |
| FR-055 | MCP Server | The MCP endpoint authenticates callers by Keycloak bearer token and resolves them to the same application user as the web UI; calls without a valid token are refused. | NEW | epic_10_MCP_SERVER |
| FR-056 | MCP Server | `list_products` returns the products (with team) the caller may read; `get_tree` returns a team's whole tree, optionally limited to one product. | NEW | epic_10_MCP_SERVER |
| FR-057 | MCP Server | `get_node` returns one node by type and id with its details (children summary, links, evidence and comment counts where those features exist). | NEW | epic_10_MCP_SERVER |
| FR-058 | MCP Server | `list_interviews` returns interviews for a product or team with their linked opportunities, applying the same notes-visibility rules as the web UI. | NEW | epic_10_MCP_SERVER |
| FR-059 | MCP Server | A "Connect an agent" page in the app shows the MCP endpoint URL and copy-paste client configuration, including how to obtain a token. | NEW | epic_10_MCP_SERVER |
| FR-060 | Transcripts | A team member with edit rights can add a meeting transcript to any tree node from the node's detail panel by pasting text into a dialog, with title, meeting date, attendees and body. | NEW | epic_12_MEETING_TRANSCRIPTS |
| FR-061 | Transcripts | A team member with edit rights can add a transcript by uploading a `.txt`, `.vtt` or `.srt` file, which the server parses to plain text, keeping VTT/SRT speaker labels and discarding timestamps and cue numbering. | NEW | epic_12_MEETING_TRANSCRIPTS |
| FR-062 | Transcripts | A node's detail panel lists that node's transcripts newest first, showing title, meeting date and attendees; viewers can read them but not add, edit or delete. | NEW | epic_12_MEETING_TRANSCRIPTS |
| FR-063 | Transcripts | Opening a transcript from the list shows its full text in a scrollable dialog, rendered as plain text. | NEW | epic_12_MEETING_TRANSCRIPTS |
| FR-064 | Transcripts | A user with edit rights can edit a transcript's title, date, attendees and body, and delete it; deleting affects only that transcript and never the node it hangs from. | NEW | epic_12_MEETING_TRANSCRIPTS |
| FR-065 | Transcripts | Nodes with transcripts show a transcript count badge on the canvas, alongside the comment and link indicators. | NEW | epic_12_MEETING_TRANSCRIPTS |
| FR-066 | Transcripts | MCP exposes `list_transcripts` (transcripts for a team or a single node, metadata only, no body) and `get_transcript` (one transcript with its full body), both scoped through `TeamAccessService` like every other tool. | NEW | epic_12_MEETING_TRANSCRIPTS |
