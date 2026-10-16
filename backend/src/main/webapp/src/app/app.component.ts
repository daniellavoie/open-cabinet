import { Component } from "@angular/core";
import { RouterOutlet } from "@angular/router";

@Component({
  selector: "open-cabinet",
  standalone: true,
  imports: [RouterOutlet],
  styleUrls: ["./app.component.css"],
  templateUrl: "./app.component.html",
})
export class AppComponent {}
