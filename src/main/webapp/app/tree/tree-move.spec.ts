import { describe, expect, it } from 'vitest';

import { OpportunityStatus } from '@/shared/model/enumerations/opportunity-status.model';
import { TeamRole } from '@/shared/model/enumerations/team-role.model';

import type { IOpportunityTreeNode, IOutcomeTreeNode, IProductTreeNode, ITeamTree } from './tree.model';
import { canMove, listValidTargets, validParentTypesFor } from './tree-move';

const buildTree = (): ITeamTree => ({
  id: 1,
  name: 'Team',
  description: null,
  createdDate: null,
  currentUserRole: TeamRole.EDITOR,
  canEdit: true,
  products: [
    {
      id: 100,
      name: 'Prod A',
      description: null,
      vision: null,
      archived: false,
      createdDate: null,
      outcomes: [
        {
          id: 200,
          title: 'Outcome A1',
          description: null,
          metric: null,
          targetValue: null,
          currentValue: null,
          status: null,
          startDate: null,
          targetDate: null,
          sortOrder: 0,
          opportunities: [
            {
              id: 300,
              title: 'Opp Root',
              description: null,
              status: OpportunityStatus.IDENTIFIED,
              valuerating: 3,
              complexity: 3,
              sortOrder: 0,
              parentId: null,
              children: [
                {
                  id: 301,
                  title: 'Opp Nested',
                  description: null,
                  status: OpportunityStatus.IDENTIFIED,
                  valuerating: 3,
                  complexity: 3,
                  sortOrder: 0,
                  parentId: 300,
                  children: [],
                  solutions: [{ id: 401, title: 'Sol Nested', description: null, status: null, effort: 3, sortOrder: 0 }],
                },
              ],
              solutions: [{ id: 400, title: 'Sol Root', description: null, status: null, effort: 3, sortOrder: 0 }],
            },
          ],
        },
        {
          id: 201,
          title: 'Outcome A2',
          description: null,
          metric: null,
          targetValue: null,
          currentValue: null,
          status: null,
          startDate: null,
          targetDate: null,
          sortOrder: 1,
          opportunities: [],
        },
      ],
    },
    {
      id: 101,
      name: 'Prod B',
      description: null,
      vision: null,
      archived: false,
      createdDate: null,
      outcomes: [
        {
          id: 220,
          title: 'Outcome B1',
          description: null,
          metric: null,
          targetValue: null,
          currentValue: null,
          status: null,
          startDate: null,
          targetDate: null,
          sortOrder: 0,
          opportunities: [],
        },
      ],
    },
  ],
});

const findOutcome = (t: ITeamTree, id: number): IOutcomeTreeNode =>
  t.products.flatMap(p => p.outcomes).find(o => o.id === id) as IOutcomeTreeNode;

describe('tree-move.validParentTypesFor', () => {
  it('outcome may re-parent only under a product', () => {
    expect(validParentTypesFor('outcome')).toEqual(['product']);
  });
  it('opportunity may re-parent under an outcome or opportunity', () => {
    expect(validParentTypesFor('opportunity').sort()).toEqual(['opportunity', 'outcome'].sort());
  });
  it('solution may re-parent only under an opportunity', () => {
    expect(validParentTypesFor('solution')).toEqual(['opportunity']);
  });
  it('product cannot be re-parented (moves only along the top row via reorder)', () => {
    expect(validParentTypesFor('product')).toEqual([]);
  });
});

describe('tree-move.canMove', () => {
  it('accepts a valid outcome -> product move to a different product', () => {
    const tree = buildTree();
    const outcome = findOutcome(tree, 200);
    expect(
      canMove({
        nodeType: 'outcome',
        nodeId: 200,
        node: outcome,
        currentParentType: 'product',
        currentParentId: 100,
        targetParentType: 'product',
        targetParentId: 101,
      }),
    ).toBe(true);
  });

  it('rejects a move onto the wrong parent type (outcome -> outcome)', () => {
    const tree = buildTree();
    const outcome = findOutcome(tree, 200);
    expect(
      canMove({
        nodeType: 'outcome',
        nodeId: 200,
        node: outcome,
        currentParentType: 'product',
        currentParentId: 100,
        targetParentType: 'outcome',
        targetParentId: 220,
      }),
    ).toBe(false);
  });

  it('rejects a move onto itself', () => {
    const tree = buildTree();
    const opp = tree.products[0].outcomes[0].opportunities[0] as IOpportunityTreeNode;
    expect(
      canMove({
        nodeType: 'opportunity',
        nodeId: 300,
        node: opp,
        currentParentType: 'outcome',
        currentParentId: 200,
        targetParentType: 'opportunity',
        targetParentId: 300,
      }),
    ).toBe(false);
  });

  it('rejects a move into its own descendant', () => {
    const tree = buildTree();
    const opp = tree.products[0].outcomes[0].opportunities[0] as IOpportunityTreeNode;
    expect(
      canMove({
        nodeType: 'opportunity',
        nodeId: 300,
        node: opp,
        currentParentType: 'outcome',
        currentParentId: 200,
        targetParentType: 'opportunity',
        targetParentId: 301,
      }),
    ).toBe(false);
  });

  it('rejects a no-op move to the current parent', () => {
    const tree = buildTree();
    const opp = tree.products[0].outcomes[0].opportunities[0] as IOpportunityTreeNode;
    expect(
      canMove({
        nodeType: 'opportunity',
        nodeId: 300,
        node: opp,
        currentParentType: 'outcome',
        currentParentId: 200,
        targetParentType: 'outcome',
        targetParentId: 200,
      }),
    ).toBe(false);
  });

  it('accepts a solution -> different opportunity', () => {
    const tree = buildTree();
    const sol = tree.products[0].outcomes[0].opportunities[0].solutions[0];
    expect(
      canMove({
        nodeType: 'solution',
        nodeId: sol.id,
        node: sol,
        currentParentType: 'opportunity',
        currentParentId: 300,
        targetParentType: 'opportunity',
        targetParentId: 301,
      }),
    ).toBe(true);
  });
});

describe('tree-move.listValidTargets', () => {
  it('lists only different products for an outcome, keeping its subtree implicit', () => {
    const tree = buildTree();
    const outcome = findOutcome(tree, 200);
    const targets = listValidTargets({
      tree,
      nodeType: 'outcome',
      nodeId: 200,
      node: outcome,
      currentParentType: 'product',
      currentParentId: 100,
    });
    // Outcome may only sit under a product; product 100 is its current parent so must be excluded.
    expect(targets.map(t => `${t.parentType}:${t.parentId}`)).toEqual(['product:101']);
  });

  it('lists every outcome and every opportunity except self/descendants/current parent for an opportunity', () => {
    const tree = buildTree();
    const opp = tree.products[0].outcomes[0].opportunities[0] as IOpportunityTreeNode;
    const targets = listValidTargets({
      tree,
      nodeType: 'opportunity',
      nodeId: 300,
      node: opp,
      currentParentType: 'outcome',
      currentParentId: 200,
    });
    const keys = targets.map(t => `${t.parentType}:${t.parentId}`).sort();
    // outcome:200 is current parent, opportunity:300 is self, opportunity:301 is descendant → all excluded.
    // outcome:201 and outcome:220 are valid outcomes; opportunity ancestors: none other exist.
    expect(keys).toEqual(['outcome:201', 'outcome:220'].sort());
  });

  it('lists nothing for a non-movable product', () => {
    const tree = buildTree();
    const targets = listValidTargets({
      tree,
      nodeType: 'product',
      nodeId: 100,
      node: tree.products[0] as IProductTreeNode,
      currentParentType: 'team',
      currentParentId: null,
    });
    expect(targets).toEqual([]);
  });

  it('groups results by product with a productName tag so deep trees stay readable', () => {
    const tree = buildTree();
    const sol = tree.products[0].outcomes[0].opportunities[0].solutions[0];
    const targets = listValidTargets({
      tree,
      nodeType: 'solution',
      nodeId: sol.id,
      node: sol,
      currentParentType: 'opportunity',
      currentParentId: 300,
    });
    // Solutions may only sit under an opportunity. The only other opportunity is 301 (child of 300)
    // which is NOT a descendant of the solution itself.
    expect(targets).toHaveLength(1);
    expect(targets[0]).toMatchObject({ parentType: 'opportunity', parentId: 301, productName: 'Prod A' });
  });
});
