import { Component } from "@angular/core";

import { AuthService } from "../security/auth-service";

@Component({
  selector: "home",
  standalone: true,
  imports: [],
  styleUrls: ["./home.component.css"],
  templateUrl: "./home.component.html",
})
export class HomeComponent {
  constructor(public authService: AuthService) {}

  logout() {
    this.authService.logout();
  }
}
