import { DatePipe, NgFor, NgIf } from "@angular/common";
import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { finalize, forkJoin, tap } from "rxjs";

import { ClientService } from "../../client/client-service";
import { Client } from "../../client/model/client";
import { Project } from "../model/project";
import { ProjectService } from "../project-service";

@Component({
  selector: "projects",
  standalone: true,
  imports: [DatePipe, FormsModule, NgFor, NgIf, RouterLink],
  styleUrls: ["./projects.component.css"],
  templateUrl: "./projects.component.html",
})
export class ProjectsComponent {
  projects?: Project[];
  clients: Client[] = [];
  clientNames = new Map<string, string>();
  name = "";
  clientId = "";
  creating = false;
  error = "";

  constructor(
    private projectService: ProjectService,
    private clientService: ClientService,
    private router: Router,
  ) {
    forkJoin([this.projectService.findAll(), this.clientService.findAll()])
      .pipe(tap(([projects, clients]) => {
        this.projects = projects;
        this.clients = clients;
        this.clientNames = new Map(clients.map(client => [client.id, client.name]));
      }))
      .subscribe();
  }

  create() {
    this.creating = true;
    this.error = "";
    this.projectService.create(this.clientId, this.name)
      .pipe(tap(project => this.router.navigate(["/projects", project.id])))
      .pipe(finalize(() => this.creating = false))
      .subscribe({ error: () => this.error = $localize`:@@projects.createError:Impossible de créer le projet.` });
  }
}
