# PRD: Ombuto Opportunity Solution Tree

## 1. What It Is

Ombuto OST is a web application for product development organisations that
practise continuous discovery as described by Teresa Torres. Each team maintains
an Opportunity Solution Tree per product: a desired outcome at the top, the
customer opportunities beneath it, the solutions being considered, and the
assumptions and experiments that test them. Teams edit the same tree in real
time, and outcomes and opportunities are visible to everyone who should see
them. It replaces trees drawn in whiteboard tools, which have no structure, go
stale, and are invisible outside the team that drew them. Tree nodes link to
Jira tickets and Confluence pages, users sign in through company SSO, and the
data is available to LLM agent tools through an MCP server.

## 2. Who Will Use It

A member of a product trio (product manager, designer or tech lead) in a company
with several product teams. They interview customers most weeks and need
somewhere to record what they learned, turn it into opportunities, and track
which solutions and assumptions they are testing. They are comfortable with
tools like Jira, Confluence and Miro, and they work in a browser at their desk,
often with the rest of the trio in the same tree at the same time. Today their
tree is a whiteboard drawing that falls out of date within weeks, that nobody
outside the team looks at, and that has no connection to the tickets and pages
where the work happens. Heads of product and similar leaders are secondary
users: they get a read-only overview across all teams' trees, but do not edit
them.

## 3. Must-Have Features

- Tree editor — build and rearrange the tree visually, from Outcome through
  nested Opportunities and Solutions to Assumptions and Experiments, with a
  status on each node.
- Real-time collaboration — several people edit the same tree at once and see
  each other's changes immediately, with threaded comments on nodes.
- Teams and products — many teams, each with many products, and one tree per
  product. A user can belong to several teams, with an owner, editor or viewer
  role in each.
- Team-scoped access — users see only the trees of teams they belong to.
- Interviews as evidence — log customer interviews and link them to the
  opportunities they support.
- Jira and Confluence links — paste a URL onto an opportunity or solution, and
  it is shown as a named, typed link.
- SSO sign-in — log in through the company identity provider.
- MCP server — read-only access to trees for LLM agent tools, limited to what
  the calling user is allowed to see.

## 4. Open Questions

- Is this one deployment per organisation, or a hosted service shared by many
  organisations?
- Is SSO the only way to sign in, or are local accounts also supported?
