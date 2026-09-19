import { type HistoryEventType } from '@/shared/model/enumerations/history-event-type.model';
import { type TreeNodeType } from '@/shared/model/enumerations/tree-node-type.model';
import { type IUser } from '@/shared/model/user.model';

export interface INodeHistory {
  id?: number;
  nodeType?: keyof typeof TreeNodeType;
  nodeId?: number;
  eventType?: keyof typeof HistoryEventType;
  summary?: string;
  createdDate?: Date;
  author?: IUser | null;
}

export class NodeHistory implements INodeHistory {
  constructor(
    public id?: number,
    public nodeType?: keyof typeof TreeNodeType,
    public nodeId?: number,
    public eventType?: keyof typeof HistoryEventType,
    public summary?: string,
    public createdDate?: Date,
    public author?: IUser | null,
  ) {}
}
