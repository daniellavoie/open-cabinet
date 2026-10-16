export type Role = "OWNER" | "DESIGNER";

export class Member {
  id!: string;
  shopId!: string;
  name!: string;
  email!: string | null;
  role!: Role;
}
