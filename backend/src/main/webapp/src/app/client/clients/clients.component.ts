import { NgFor, NgIf } from "@angular/common";
import { Component } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize, tap } from "rxjs";

import { ClientRequest, ClientService } from "../client-service";
import { Client } from "../model/client";

@Component({
  selector: "clients",
  standalone: true,
  imports: [FormsModule, NgFor, NgIf],
  styleUrls: ["./clients.component.css"],
  templateUrl: "./clients.component.html",
})
export class ClientsComponent {
  clients?: Client[];
  editing: Client | null = null;
  form: ClientRequest = emptyForm();
  saving = false;
  error = "";

  constructor(private clientService: ClientService) {
    this.load();
  }

  load() {
    this.clientService.findAll()
      .pipe(tap(clients => this.clients = clients))
      .subscribe();
  }

  edit(client: Client | null) {
    this.editing = client;
    this.form = client ? { name: client.name, email: client.email, phone: client.phone, address: client.address } : emptyForm();
    this.error = "";
  }

  save() {
    this.saving = true;
    const request = this.editing
      ? this.clientService.update(this.editing.id, this.form)
      : this.clientService.create(this.form);

    request
      .pipe(tap(() => {
        this.edit(null);
        this.load();
      }))
      .pipe(finalize(() => this.saving = false))
      .subscribe({ error: () => this.error = $localize`:@@clients.saveError:Impossible d'enregistrer le client.` });
  }
}

function emptyForm(): ClientRequest {
  return { name: "", email: null, phone: null, address: null };
}
