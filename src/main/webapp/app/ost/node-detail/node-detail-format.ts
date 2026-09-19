/*
 * Presentation helpers for the full-page node detail (prototype `Ombuto OST.dc.html`, view
 * "detail": status tag + metric tags beside the title).
 */
import { evidenceStrength } from '../domain/derive';
import { priorityColor, priorityLabel, valueLabel } from '../domain/rules';
import type { OstNode } from '../domain/types';
import { confidenceLabel } from '../panel/panel-format';

export interface MetricTag {
  /** stable id, used in data-cy (`ost-node-detail-metric-{id}`) */
  id: 'priority' | 'value' | 'evidence' | 'confidence';
  label: string;
  /** optional colour dot (priority ramp) */
  dot?: string;
  /** accessible description of the tag */
  title: string;
}

/**
 * The metric tags of a node, one per signal its type carries (as on the canvas node):
 * opportunity → priority + value, solution → evidence strength (derived), assumption → confidence.
 */
export function metricTags(node: OstNode, nodes: OstNode[]): MetricTag[] {
  switch (node.type) {
    case 'opportunity':
      return [
        {
          id: 'priority',
          label: `${priorityLabel(node.priority)} priority`,
          dot: priorityColor(node.priority),
          title: `Priority ${node.priority} of 100`,
        },
        { id: 'value', label: `${'$'.repeat(node.value)} ${valueLabel(node.value)}`, title: `Value ${node.value} of 5` },
      ];
    case 'solution': {
      const { score, tests } = evidenceStrength(node.id, nodes);
      return [
        {
          id: 'evidence',
          label: score === null ? 'untested' : `${score}% evidence`,
          title: score === null ? 'No assumption tests yet' : `Evidence strength from ${tests} assumption test${tests === 1 ? '' : 's'}`,
        },
      ];
    }
    case 'assumption':
      return [{ id: 'confidence', label: `${confidenceLabel(node.conf)} · ${node.conf}%`, title: `Confidence ${node.conf}%` }];
    default:
      return [];
  }
}
