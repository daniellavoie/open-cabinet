import { Role } from "../../member/model/member";

export class Invitation {
  id!: string;
  email!: string | null;
  role!: Role;
  expirationTimestamp!: string;
  creationTimestamp!: string;
}

export class CreatedInvitation {
  invitation!: Invitation;
  token!: string;
}

export class InvitationPreview {
  shopName!: string;
  role!: Role;
  invitedByName!: string;
}
