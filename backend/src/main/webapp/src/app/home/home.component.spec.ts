import { TestBed } from "@angular/core/testing";

import { AuthService } from "../security/auth-service";
import { HomeComponent } from "./home.component";

describe("HomeComponent", () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: AuthService, useValue: { displayName: () => "Marie", logout: () => Promise.resolve() } },
      ],
    }).compileComponents();
  });

  it("greets the signed-in Member", async () => {
    const fixture = TestBed.createComponent(HomeComponent);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector("p")?.textContent).toContain("Marie");
  });
});
