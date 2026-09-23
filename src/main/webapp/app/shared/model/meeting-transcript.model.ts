import { type IAssumption } from '@/shared/model/assumption.model';
import { type MeetingTranscriptSource } from '@/shared/model/enumerations/meeting-transcript-source.model';
import { type IEvidence } from '@/shared/model/evidence.model';
import { type IOpportunity } from '@/shared/model/opportunity.model';
import { type IOutcome } from '@/shared/model/outcome.model';
import { type IProduct } from '@/shared/model/product.model';
import { type ISolution } from '@/shared/model/solution.model';
import { type IUser } from '@/shared/model/user.model';
export interface IMeetingTranscript {
  id?: number;
  title?: string;
  meetingDate?: Date;
  attendees?: string | null;
  body?: string;
  source?: keyof typeof MeetingTranscriptSource;
  createdDate?: Date;
  editedDate?: Date | null;
  author?: IUser | null;
  product?: IProduct | null;
  outcome?: IOutcome | null;
  opportunity?: IOpportunity | null;
  solution?: ISolution | null;
  assumption?: IAssumption | null;
  evidence?: IEvidence | null;
}

export class MeetingTranscript implements IMeetingTranscript {
  constructor(
    public id?: number,
    public title?: string,
    public meetingDate?: Date,
    public attendees?: string | null,
    public body?: string,
    public source?: keyof typeof MeetingTranscriptSource,
    public createdDate?: Date,
    public editedDate?: Date | null,
    public author?: IUser | null,
    public product?: IProduct | null,
    public outcome?: IOutcome | null,
    public opportunity?: IOpportunity | null,
    public solution?: ISolution | null,
    public assumption?: IAssumption | null,
    public evidence?: IEvidence | null,
  ) {}
}
