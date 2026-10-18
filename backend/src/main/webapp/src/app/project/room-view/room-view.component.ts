import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, NgZone, OnChanges, OnDestroy, Output, ViewChild } from "@angular/core";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";

import { Material } from "../../catalog/model/material";
import { ConstructionStandard } from "../../shop/model/shop";
import { assemble, cabinetContext } from "../assembly";
import { doorLeaves, frontRows, frontWidth } from "../front-layout";
import { Appliance, Cabinet, CabinetTemplate, Design, Filler, Opening, standsOnFloor, templateKey } from "../model/design";
import { footprintCentre, layoutWalls, OnWall, WallPlacement } from "../room-layout";

const MM = 1 / 1000;
const WALL_THICKNESS_MM = 100;
const FRONT_THICKNESS_MM = 19;
const FRONT_GAP_MM = 3;
const TOE_KICK_SETBACK_MM = 75;
const OPENING_DEPTH_MM = 40;
const CLICK_TOLERANCE_PX = 4;
const OPENING_THICKNESS_MM = 20;
const PANEL_THICKNESS_MM = 19;
const SHELF_SETBACK_MM = 25;
const CROWN_HEIGHT_MM = 76.2;
const CROWN_PROJECTION_MM = 50;
const UPDATE_MARKER_RADIUS_MM = 40;

/**
 * A Cabinet, Opening or Appliance dragged along its Wall, or with
 * `dividerIndex`, one of a Cabinet's dividers, `offsetMm` from its left side.
 */
export interface ItemMove {
  id: string;
  offsetMm: number;
  done: boolean;
  dividerIndex?: number;
}

interface Drag {
  id: string;
  placement: WallPlacement;
  grabAlongMm: number;
  dividerIndex?: number;
}

interface Pick {
  id: string;
  dividerIndex?: number;
}

/**
 * The Room, its Openings, Appliances and Cabinets in 3D. Each is picked by
 * clicking and dragged along its Wall; the editor applies the move and the
 * Construction Rules, then hands back the updated design. The selected
 * Cabinet turns see-through to show its dividers and shelves, and its
 * dividers can be dragged too.
 */
@Component({
  selector: "room-view",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [],
  styleUrls: ["./room-view.component.css"],
  templateUrl: "./room-view.component.html",
})
export class RoomViewComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild("canvas", { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  @Input() design!: Design;
  @Input() templates: CabinetTemplate[] = [];
  @Input() selectedId: string | null = null;
  /**
   * Cabinets, Openings and Appliances breaking a Construction Rule.
   */
  @Input() invalidIds = new Set<string>();
  /**
   * Cabinets on an older Template Version than their template's newest.
   */
  @Input() outdatedIds = new Set<string>();
  @Input() toeKickHeightMm = 101.6;
  /** For assembled templates: the Shop's Standard and catalog give their materials' thicknesses. */
  @Input() standard: ConstructionStandard | null = null;
  @Input() materials: Material[] = [];
  /**
   * Things can still be picked, but not dragged.
   */
  @Input() readOnly = false;

  @Output() selectItem = new EventEmitter<string | null>();
  @Output() move = new EventEmitter<ItemMove>();

  private renderer?: THREE.WebGLRenderer;
  private scene = new THREE.Scene();
  private camera = new THREE.PerspectiveCamera(45, 1, 0.05, 100);
  private controls?: OrbitControls;
  private content = new THREE.Group();
  private raycaster = new THREE.Raycaster();
  private frame = 0;
  private resizeObserver?: ResizeObserver;
  private framedWallCount = -1;
  private drag: Drag | null = null;
  private pointerDown: { x: number; y: number } | null = null;
  private listeners: [string, EventListener][] = [];

  constructor(
    private host: ElementRef<HTMLElement>,
    private ngZone: NgZone,
  ) {}

  ngAfterViewInit() {
    this.ngZone.runOutsideAngular(() => {
      const canvas = this.canvasRef.nativeElement;
      this.renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
      this.renderer.setPixelRatio(window.devicePixelRatio);

      this.scene.add(new THREE.HemisphereLight(0xffffff, 0x888888, 2.2));
      const sun = new THREE.DirectionalLight(0xffffff, 1.4);
      sun.position.set(4, 8, 6);
      this.scene.add(sun);
      this.scene.add(this.content);

      this.controls = new OrbitControls(this.camera, canvas);
      this.controls.enableDamping = true;
      this.controls.maxPolarAngle = Math.PI / 2 - 0.02;

      this.listen("pointerdown", event => this.onPointerDown(event as PointerEvent));
      this.listen("pointermove", event => this.onPointerMove(event as PointerEvent));
      this.listen("pointerup", event => this.onPointerUp(event as PointerEvent));

      this.resizeObserver = new ResizeObserver(() => this.resize());
      this.resizeObserver.observe(this.host.nativeElement);
      this.resize();
      this.rebuild();

      const loop = () => {
        this.frame = requestAnimationFrame(loop);
        this.controls?.update();
        this.renderer?.render(this.scene, this.camera);
      };
      loop();
    });
  }

  ngOnChanges() {
    if (this.renderer) {
      this.rebuild();
    }
  }

  ngOnDestroy() {
    cancelAnimationFrame(this.frame);
    this.resizeObserver?.disconnect();
    this.listeners.forEach(([type, listener]) => this.canvasRef.nativeElement.removeEventListener(type, listener));
    this.controls?.dispose();
    this.clearContent();
    this.renderer?.dispose();
  }

  private listen(type: string, listener: EventListener) {
    this.canvasRef.nativeElement.addEventListener(type, listener);
    this.listeners.push([type, listener]);
  }

  private resize() {
    const { clientWidth, clientHeight } = this.host.nativeElement;
    if (!this.renderer || clientWidth === 0 || clientHeight === 0) {
      return;
    }
    this.renderer.setSize(clientWidth, clientHeight, false);
    this.camera.aspect = clientWidth / clientHeight;
    this.camera.updateProjectionMatrix();
  }

  private rebuild() {
    this.clearContent();
    const colors = readColors(this.host.nativeElement);
    this.scene.background = new THREE.Color(colors.background);

    const placements = layoutWalls(this.design.walls);
    const placementsById = new Map(placements.map(placement => [placement.wall.id, placement]));
    const templates = new Map(this.templates.map(template => [templateKey(template.code, template.version), template]));

    const bounds = new THREE.Box3();
    for (const placement of placements) {
      const wall = this.wallMesh(placement, colors.wall);
      this.content.add(wall);
      bounds.expandByObject(wall);
    }

    const floorSize = bounds.isEmpty() ? new THREE.Vector3(4, 0, 4) : bounds.getSize(new THREE.Vector3());
    const floorCentre = bounds.isEmpty() ? new THREE.Vector3() : bounds.getCenter(new THREE.Vector3());
    const floor = new THREE.Mesh(
      new THREE.PlaneGeometry(floorSize.x + 2, floorSize.z + 2),
      new THREE.MeshStandardMaterial({ color: colors.floor }),
    );
    floor.rotation.x = -Math.PI / 2;
    floor.position.set(floorCentre.x, 0, floorCentre.z);
    this.content.add(floor);

    const highlightOf = (id: string) => this.invalidIds.has(id) ? colors.invalid : id === this.selectedId ? colors.selected : null;

    for (const opening of this.design.openings ?? []) {
      const placement = placementsById.get(opening.wallId);
      if (placement) {
        this.content.add(this.openingMesh(opening, placement, colors, highlightOf(opening.id)));
      }
    }

    for (const appliance of this.design.appliances ?? []) {
      const placement = placementsById.get(appliance.wallId);
      if (placement) {
        this.content.add(this.applianceMesh(appliance, placement, colors, highlightOf(appliance.id)));
      }
    }

    for (const filler of this.design.fillers ?? []) {
      const placement = placementsById.get(filler.wallId);
      if (placement) {
        this.content.add(this.fillerMesh(filler, placement, colors, highlightOf(filler.id)));
      }
    }

    for (const cabinet of this.design.cabinets) {
      const placement = placementsById.get(cabinet.wallId);
      const template = templates.get(templateKey(cabinet.templateCode, cabinet.templateVersion));
      if (!placement || !template) {
        continue;
      }
      const mesh = this.cabinetMesh(cabinet, template, placement, colors, highlightOf(cabinet.id));
      if (this.outdatedIds.has(cabinet.id)) {
        mesh.add(updateMarker(cabinet, colors.outdated));
      }
      this.content.add(tagged(mesh, cabinet.id));
    }

    if (placements.length !== this.framedWallCount && !bounds.isEmpty()) {
      this.framedWallCount = placements.length;
      this.frame3d(bounds);
    }
  }

  private frame3d(bounds: THREE.Box3) {
    const centre = bounds.getCenter(new THREE.Vector3());
    const size = bounds.getSize(new THREE.Vector3()).length();
    this.controls!.target.set(centre.x, 1, centre.z);
    this.camera.position.set(centre.x + size * 0.2, size * 0.75 + 1.5, centre.z + size * 1.1 + 1);
    this.camera.updateProjectionMatrix();
  }

  private wallMesh(placement: WallPlacement, color: string): THREE.Mesh {
    const { wall, start, direction, normal } = placement;
    const mesh = new THREE.Mesh(
      new THREE.BoxGeometry(wall.lengthMm * MM, wall.heightMm * MM, WALL_THICKNESS_MM * MM),
      new THREE.MeshStandardMaterial({ color, transparent: true, opacity: 0.55 }),
    );
    mesh.position.set(
      (start.x + direction.x * wall.lengthMm / 2 - normal.x * WALL_THICKNESS_MM / 2) * MM,
      wall.heightMm * MM / 2,
      (start.z + direction.z * wall.lengthMm / 2 - normal.z * WALL_THICKNESS_MM / 2) * MM,
    );
    mesh.rotation.y = -placement.angleRadians;
    return mesh;
  }

  /**
   * A group in a Wall item's own frame: x along the Wall, y up from the
   * item's elevation, z out of the Wall, centred on a footprint `depthMm` deep.
   */
  private onWallGroup(item: OnWall & { elevationMm: number }, depthMm: number, placement: WallPlacement): THREE.Group {
    const group = new THREE.Group();
    const centre = footprintCentre(item, depthMm, placement);
    group.position.set(centre.x * MM, item.elevationMm * MM, centre.z * MM);
    group.rotation.y = -placement.angleRadians;
    return group;
  }

  /**
   * A window as a pale pane on the Wall, a doorway as a dark one.
   */
  private openingMesh(opening: Opening, placement: WallPlacement, colors: Colors, highlight: string | null): THREE.Group {
    const group = this.onWallGroup(opening, OPENING_THICKNESS_MM, placement);
    const pane = box(opening.widthMm, opening.heightMm, OPENING_THICKNESS_MM, opening.kind === "WINDOW" ? colors.window : colors.doorway);
    pane.position.set(0, opening.heightMm / 2 * MM, 0);
    group.add(pane);
    this.outline(group, opening.widthMm, opening.heightMm, OPENING_THICKNESS_MM, highlight);
    return tagged(group, opening.id);
  }

  private applianceMesh(appliance: Appliance, placement: WallPlacement, colors: Colors, highlight: string | null): THREE.Group {
    const group = this.onWallGroup(appliance, appliance.depthMm, placement);
    const body = box(appliance.widthMm, appliance.heightMm, appliance.depthMm, colors.appliance);
    body.position.set(0, appliance.heightMm / 2 * MM, 0);
    group.add(body);
    this.outline(group, appliance.widthMm, appliance.heightMm, appliance.depthMm, highlight ?? colors.toeKick);
    return tagged(group, appliance.id);
  }

  /**
   * A Filler as a panel of Front material, its face flush with the Fronts.
   */
  private fillerMesh(filler: Filler, placement: WallPlacement, colors: Colors, highlight: string | null): THREE.Group {
    const group = this.onWallGroup(filler, filler.depthMm, placement);
    const panel = box(filler.widthMm, filler.heightMm, FRONT_THICKNESS_MM, colors.front);
    panel.position.set(0, filler.heightMm / 2 * MM, (filler.depthMm / 2 - FRONT_THICKNESS_MM / 2) * MM);
    group.add(panel);
    this.outline(group, filler.widthMm, filler.heightMm, filler.depthMm, highlight);
    return tagged(group, filler.id);
  }

  private outline(group: THREE.Group, widthMm: number, heightMm: number, depthMm: number, color: string | null) {
    if (!color) {
      return;
    }
    const outline = new THREE.LineSegments(
      new THREE.EdgesGeometry(new THREE.BoxGeometry(widthMm * MM, heightMm * MM, depthMm * MM)),
      new THREE.LineBasicMaterial({ color }),
    );
    outline.position.set(0, heightMm / 2 * MM, 0);
    group.add(outline);
  }

  /**
   * A Cabinet as a group in its own frame: x along the Wall, y up, z out of
   * the Wall towards its front.
   */
  private cabinetMesh(cabinet: Cabinet, template: CabinetTemplate, placement: WallPlacement, colors: Colors, highlight: string | null): THREE.Group {
    if (template.assembly && this.standard) {
      return this.assembledMesh(cabinet, template, placement, colors, highlight);
    }
    const group = this.onWallGroup(cabinet, cabinet.depthMm, placement);

    const base = standsOnFloor(template.type);
    const boxBottom = base ? this.toeKickHeightMm : 0;
    const boxHeight = cabinet.heightMm - boxBottom;
    const boxDepth = cabinet.depthMm - FRONT_THICKNESS_MM;

    const interior = cabinet.id === this.selectedId;
    const carcass = box(cabinet.widthMm, boxHeight, boxDepth, colors.carcass, interior ? 0.3 : 1);
    carcass.position.set(0, (boxBottom + boxHeight / 2) * MM, -FRONT_THICKNESS_MM / 2 * MM);
    group.add(carcass);

    if (interior) {
      this.addInterior(group, cabinet, boxBottom, boxHeight, boxDepth, colors);
    }

    if (base && boxBottom > 0) {
      const toeKick = box(cabinet.widthMm, boxBottom, boxDepth - TOE_KICK_SETBACK_MM, colors.toeKick);
      toeKick.position.set(0, boxBottom / 2 * MM, -(FRONT_THICKNESS_MM + TOE_KICK_SETBACK_MM) / 2 * MM);
      group.add(toeKick);
    }

    for (const front of fronts(cabinet, template, boxHeight)) {
      const depth = front.opening ? OPENING_DEPTH_MM : FRONT_THICKNESS_MM;
      const mesh = box(front.width, front.height, depth, front.opening ? colors.toeKick : colors.front, interior && !front.opening ? 0.25 : 1);
      mesh.position.set(front.x * MM, (boxBottom + front.y) * MM, (cabinet.depthMm / 2 - depth / 2) * MM);
      group.add(mesh);
    }

    const ends = [cabinet.finishedLeft ? -1 : 0, cabinet.finishedRight ? 1 : 0].filter(side => side !== 0);
    for (const side of ends) {
      const panel = box(FRONT_THICKNESS_MM, cabinet.heightMm, cabinet.depthMm, colors.front);
      panel.position.set(side * (cabinet.widthMm + FRONT_THICKNESS_MM) / 2 * MM, cabinet.heightMm / 2 * MM, 0);
      group.add(panel);
    }

    if (cabinet.crown && template.type !== "BASE") {
      const crown = box(cabinet.widthMm, CROWN_HEIGHT_MM, CROWN_PROJECTION_MM, colors.front);
      crown.position.set(0, (cabinet.heightMm + CROWN_HEIGHT_MM / 2) * MM, cabinet.depthMm / 2 * MM);
      group.add(crown);
    }

    this.outline(group, cabinet.widthMm, cabinet.heightMm, cabinet.depthMm, highlight);
    return tagged(group, cabinet.id);
  }

  /**
   * An assembled Cabinet drawn as its own Parts (ADR 0006), from the same
   * assembly as its Cut List; Fronts turn see-through when it is selected.
   */
  private assembledMesh(cabinet: Cabinet, template: CabinetTemplate, placement: WallPlacement, colors: Colors, highlight: string | null): THREE.Group {
    const group = this.onWallGroup(cabinet, cabinet.depthMm, placement);
    const interior = cabinet.id === this.selectedId;
    const result = assemble(template.assembly!, cabinetContext(cabinet, this.standard!, this.materials));
    for (const part of result.parts) {
      const { x0, x1, y0, y1, z0, z1 } = part.box;
      const mesh = box(x1 - x0, y1 - y0, z1 - z0, part.front ? colors.front : colors.carcass, interior && part.front ? 0.25 : 1);
      mesh.position.set(((x0 + x1) / 2 - cabinet.widthMm / 2) * MM, (y0 + y1) / 2 * MM, ((z0 + z1) / 2 - cabinet.depthMm / 2) * MM);
      group.add(mesh);
    }
    this.outline(group, cabinet.widthMm, cabinet.heightMm, cabinet.depthMm, highlight);
    return tagged(group, cabinet.id);
  }

  /**
   * Dividers, tagged with their index for dragging, and the shelves of each
   * section, evenly spaced.
   */
  private addInterior(group: THREE.Group, cabinet: Cabinet, boxBottom: number, boxHeight: number, boxDepth: number, colors: Colors) {
    const dividers = cabinet.dividerPositionsMm ?? [];
    const inside = boxHeight - 2 * PANEL_THICKNESS_MM;
    const z = -FRONT_THICKNESS_MM / 2 * MM;
    dividers.forEach((position, index) => {
      const divider = box(PANEL_THICKNESS_MM, inside, boxDepth, colors.front);
      divider.position.set((position - cabinet.widthMm / 2) * MM, (boxBottom + boxHeight / 2) * MM, z);
      divider.userData["dividerIndex"] = index;
      group.add(divider);
    });

    const bounds = [PANEL_THICKNESS_MM / 2, ...dividers, cabinet.widthMm - PANEL_THICKNESS_MM / 2];
    for (let section = 0; section < bounds.length - 1; section++) {
      const left = bounds[section] + PANEL_THICKNESS_MM / 2;
      const right = bounds[section + 1] - PANEL_THICKNESS_MM / 2;
      for (let shelf = 1; shelf <= cabinet.shelfCount; shelf++) {
        const mesh = box(Math.max(right - left, 1), PANEL_THICKNESS_MM, boxDepth - SHELF_SETBACK_MM, colors.carcass);
        const y = boxBottom + PANEL_THICKNESS_MM + inside * shelf / (cabinet.shelfCount + 1);
        mesh.position.set(((left + right) / 2 - cabinet.widthMm / 2) * MM, y * MM, z - SHELF_SETBACK_MM / 2 * MM);
        group.add(mesh);
      }
    }
  }

  private clearContent() {
    this.content.traverse(object => {
      const mesh = object as THREE.Mesh;
      mesh.geometry?.dispose();
      const material = mesh.material as THREE.Material | THREE.Material[] | undefined;
      (Array.isArray(material) ? material : material ? [material] : []).forEach(item => item.dispose());
    });
    this.content.clear();
  }

  private onPointerDown(event: PointerEvent) {
    this.pointerDown = { x: event.clientX, y: event.clientY };
    const picked = this.pick(event);
    const item: OnWall | undefined = [...this.design.cabinets, ...this.design.openings ?? [], ...this.design.appliances ?? [], ...this.design.fillers ?? []]
      .find(candidate => candidate.id === picked?.id);
    if (!picked || !item) {
      return;
    }

    const placement = layoutWalls(this.design.walls).find(candidate => candidate.wall.id === item.wallId);
    const along = placement ? this.alongWall(event, placement) : null;
    this.ngZone.run(() => this.selectItem.emit(item.id));

    if (placement && along !== null && !this.readOnly) {
      const dividerIndex = picked.dividerIndex;
      const cabinet = this.design.cabinets.find(candidate => candidate.id === item.id);
      const grabbed = dividerIndex !== undefined && cabinet
        ? item.offsetMm + (cabinet.dividerPositionsMm ?? [])[dividerIndex]
        : item.offsetMm;
      this.drag = { id: item.id, placement, grabAlongMm: along - grabbed, dividerIndex };
      this.controls!.enabled = false;
      this.canvasRef.nativeElement.setPointerCapture(event.pointerId);
    }
  }

  private onPointerMove(event: PointerEvent) {
    if (!this.drag) {
      return;
    }
    const along = this.alongWall(event, this.drag.placement);
    if (along !== null) {
      const move = this.dragMove(this.drag, along, false);
      this.ngZone.run(() => this.move.emit(move));
    }
  }

  private onPointerUp(event: PointerEvent) {
    if (this.drag) {
      const drag = this.drag;
      this.drag = null;
      this.controls!.enabled = true;
      this.canvasRef.nativeElement.releasePointerCapture(event.pointerId);
      const along = this.alongWall(event, drag.placement);
      if (along !== null) {
        const move = this.dragMove(drag, along, true);
        this.ngZone.run(() => this.move.emit(move));
      }
    } else if (this.pointerDown
      && Math.hypot(event.clientX - this.pointerDown.x, event.clientY - this.pointerDown.y) < CLICK_TOLERANCE_PX
      && !this.pick(event)) {
      this.ngZone.run(() => this.selectItem.emit(null));
    }
    this.pointerDown = null;
  }

  private dragMove(drag: Drag, along: number, done: boolean): ItemMove {
    if (drag.dividerIndex === undefined) {
      return { id: drag.id, offsetMm: along - drag.grabAlongMm, done };
    }
    const cabinet = this.design.cabinets.find(candidate => candidate.id === drag.id)!;
    return { id: drag.id, offsetMm: along - drag.grabAlongMm - cabinet.offsetMm, done, dividerIndex: drag.dividerIndex };
  }

  /**
   * What is under the pointer: the nearest thing, or a divider of the
   * selected Cabinet, seen through its see-through box.
   */
  private pick(event: PointerEvent): Pick | null {
    this.aim(event);
    const hits = this.raycaster.intersectObjects(this.content.children, true)
      .filter(intersection => intersection.object.userData["pickId"]);
    const divider = hits.find(intersection => intersection.object.userData["dividerIndex"] !== undefined);
    if (divider) {
      return { id: divider.object.userData["pickId"] as string, dividerIndex: divider.object.userData["dividerIndex"] as number };
    }
    return hits.length > 0 ? { id: hits[0].object.userData["pickId"] as string } : null;
  }

  /**
   * Distance along a Wall, in millimetres, where the pointer meets the Wall's
   * vertical plane.
   */
  private alongWall(event: PointerEvent, placement: WallPlacement): number | null {
    this.aim(event);
    const plane = new THREE.Plane().setFromNormalAndCoplanarPoint(
      new THREE.Vector3(placement.normal.x, 0, placement.normal.z),
      new THREE.Vector3(placement.start.x * MM, 0, placement.start.z * MM),
    );
    const point = this.raycaster.ray.intersectPlane(plane, new THREE.Vector3());
    if (!point) {
      return null;
    }
    return (point.x / MM - placement.start.x) * placement.direction.x + (point.z / MM - placement.start.z) * placement.direction.z;
  }

  private aim(event: PointerEvent) {
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    const pointer = new THREE.Vector2(
      ((event.clientX - rect.left) / rect.width) * 2 - 1,
      -((event.clientY - rect.top) / rect.height) * 2 + 1,
    );
    this.raycaster.setFromCamera(pointer, this.camera);
  }
}

interface Colors {
  background: string;
  floor: string;
  wall: string;
  carcass: string;
  front: string;
  toeKick: string;
  selected: string;
  invalid: string;
  outdated: string;
  window: string;
  doorway: string;
  appliance: string;
}

/**
 * Scene colors come from the design tokens, so the 3D view follows the theme.
 */
function readColors(element: HTMLElement): Colors {
  const style = getComputedStyle(element);
  const token = (name: string) => style.getPropertyValue(name).trim() || "#888888";
  return {
    background: token("--oc-3d-background"),
    floor: token("--oc-3d-floor"),
    wall: token("--oc-3d-wall"),
    carcass: token("--oc-3d-carcass"),
    front: token("--oc-3d-front"),
    toeKick: token("--oc-3d-toe-kick"),
    selected: token("--oc-3d-selected"),
    invalid: token("--oc-3d-invalid"),
    outdated: token("--oc-3d-outdated"),
    window: token("--oc-3d-window"),
    doorway: token("--oc-3d-doorway"),
    appliance: token("--oc-3d-appliance"),
  };
}

/**
 * Marks every mesh of a group as picking the item `id`.
 */
function tagged(group: THREE.Group, id: string): THREE.Group {
  group.traverse(object => object.userData["pickId"] = id);
  return group;
}

/**
 * A marker floating over the front of a Cabinet whose template has a newer
 * version, in the Cabinet's frame.
 */
function updateMarker(cabinet: Cabinet, color: string): THREE.Mesh {
  const marker = new THREE.Mesh(
    new THREE.SphereGeometry(UPDATE_MARKER_RADIUS_MM * MM, 16, 12),
    new THREE.MeshStandardMaterial({ color }),
  );
  marker.position.set(0, (cabinet.heightMm + UPDATE_MARKER_RADIUS_MM * 3) * MM, cabinet.depthMm / 2 * MM);
  return marker;
}

function box(widthMm: number, heightMm: number, depthMm: number, color: string, opacity = 1): THREE.Mesh {
  return new THREE.Mesh(
    new THREE.BoxGeometry(widthMm * MM, heightMm * MM, depthMm * MM),
    new THREE.MeshStandardMaterial({ color, transparent: opacity < 1, opacity, depthWrite: opacity === 1 }),
  );
}

/**
 * Fronts in the Cabinet's frame: x is the centre along the Wall, y the centre
 * above the box bottom. Openings (for appliances) are drawn as dark recesses.
 */
function fronts(cabinet: Cabinet, template: CabinetTemplate, boxHeight: number): { x: number; y: number; width: number; height: number; opening: boolean }[] {
  const result: { x: number; y: number; width: number; height: number; opening: boolean }[] = [];
  const width = frontWidth(template, cabinet.widthMm);
  const blind = template.blindWidthMm ?? 0;
  const blindOnLeft = cabinet.blindSide === "LEFT";
  // Centre of the fronted part, from the cabinet's centre.
  const centre = blind === 0 ? 0 : (blindOnLeft ? blind / 2 : -blind / 2);

  for (const row of frontRows(template, boxHeight, FRONT_GAP_MM)) {
    const y = row.bottomMm + row.heightMm / 2;
    if (row.kind === "DOOR") {
      const leaves = doorLeaves(width);
      const leafWidth = (width - FRONT_GAP_MM * leaves) / leaves;
      for (let index = 0; index < leaves; index++) {
        const x = centre - width / 2 + FRONT_GAP_MM / 2 + index * (leafWidth + FRONT_GAP_MM) + leafWidth / 2;
        result.push({ x, y, width: leafWidth, height: row.heightMm, opening: false });
      }
    } else {
      result.push({ x: centre, y, width: width - FRONT_GAP_MM, height: row.heightMm, opening: row.kind === "OPENING" });
    }
  }

  if (blind > 0) {
    const x = blindOnLeft ? -cabinet.widthMm / 2 + blind / 2 : cabinet.widthMm / 2 - blind / 2;
    result.push({ x, y: boxHeight / 2, width: blind - FRONT_GAP_MM, height: boxHeight - FRONT_GAP_MM, opening: false });
  }

  return result;
}
