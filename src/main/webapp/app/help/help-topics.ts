export interface HelpTopic {
  id: string;
  title: string;
  summary: string;
  steps: string[];
  changesTree: boolean;
  roleNote?: string;
}

const EDITOR_OR_OWNER = 'You need the editor or owner role in the team that owns this tree to make these changes.';

export const helpTopics: readonly HelpTopic[] = [
  {
    id: 'signing-in',
    title: 'Signing in',
    summary: 'Sign in to reach your trees, teams and interviews.',
    steps: [
      'Open the application in your browser.',
      'Click Sign in in the top navigation bar.',
      'Enter your email address and password on the Keycloak sign-in page.',
      'You land back on the home page with your account visible in the top right.',
    ],
    changesTree: false,
  },
  {
    id: 'building-a-tree',
    title: 'Building a tree',
    summary: 'Add products, outcomes, opportunities, solutions, assumptions and experiments, and set a status on each node.',
    steps: [
      'Open Trees from the sidebar and pick the tree you want to work on.',
      'Click the + button on the canvas root to add a product.',
      'Click + on the product to add an outcome underneath it.',
      'Click + on the outcome to add an opportunity, then + on the opportunity to add a solution.',
      'Add an assumption under the solution, and add an experiment under the assumption to test it.',
      'Click a node to open its detail panel, then set the status field to reflect where the work is.',
    ],
    changesTree: true,
    roleNote: EDITOR_OR_OWNER,
  },
  {
    id: 'focusing-on-a-product',
    title: 'Focusing the canvas on one product',
    summary: 'Narrow the canvas to a single product so the tree stays readable.',
    steps: [
      'Open the tree and find the product you want to focus on.',
      'Click the product node to select it.',
      'Click Focus on the product to hide the other products and keep only this branch on the canvas.',
      'Click Clear focus in the toolbar to bring the other products back.',
    ],
    changesTree: false,
  },
  {
    id: 'teams-products-and-roles',
    title: 'Working with teams, products and roles',
    summary: 'Set up a team, add its products, and invite members as owner, editor or viewer.',
    steps: [
      'Open Teams from the sidebar and pick the team, or click Create team to make a new one.',
      'On the team page, add the products the team is responsible for.',
      'Open the Members tab and click Add member.',
      "Enter the person's email address and pick their role: owner, editor or viewer.",
      'Owners manage the team and its members, editors change the tree, viewers can only read.',
    ],
    changesTree: true,
    roleNote: 'You need the owner role in the team to add or remove members and change roles; editors and owners can add products.',
  },
  {
    id: 'real-time-editing-and-comments',
    title: 'Editing and commenting on nodes in real time',
    summary: "See other people's edits as they happen and leave comments on a node.",
    steps: [
      'Open a node on the canvas to reveal its detail panel on the right.',
      'Change the title, description or status; other people watching the tree see the change appear.',
      'Open the Comments tab in the detail panel.',
      'Type your comment and press Post to add it to the node.',
    ],
    changesTree: true,
    roleNote: EDITOR_OR_OWNER,
  },
  {
    id: 'logging-interviews',
    title: 'Logging interviews',
    summary: 'Record an interview transcript and link it to the opportunities it informed.',
    steps: [
      'Open Interviews from the sidebar and click Log interview.',
      'Fill in the interviewee, the date and the interview transcript.',
      'Click Save to store the interview against the team.',
      'Open the interview and click Link to opportunity.',
      'Pick one or more opportunities in the tree so the interview shows up on those opportunity nodes.',
    ],
    changesTree: true,
    roleNote: EDITOR_OR_OWNER,
  },
  {
    id: 'jira-and-confluence-links',
    title: 'Adding Jira and Confluence links to a node',
    summary: 'Attach a Jira ticket or Confluence page to a node so the team can jump straight to it.',
    steps: [
      'Click the node on the canvas to open its detail panel.',
      'Open the Links tab.',
      'Click Add link and pick Jira or Confluence.',
      'Paste the Jira ticket URL or the Confluence page URL and press Save.',
      'The link appears on the node with the Jira or Confluence icon and opens in a new tab when clicked.',
    ],
    changesTree: true,
    roleNote: EDITOR_OR_OWNER,
  },
  {
    id: 'connect-an-agent',
    title: 'Connecting an AI agent (MCP)',
    summary: 'Point an MCP-capable AI agent at this application so it can read your tree.',
    steps: [
      'Open Connect an agent from the sidebar.',
      'Copy the MCP endpoint URL shown on the page.',
      "Paste the URL into your agent's MCP server configuration.",
      'Sign in from the agent when it asks you to, using the same account you use here.',
      'The agent can now list products, read the tree and look up interviews on your behalf.',
    ],
    changesTree: false,
  },
];
