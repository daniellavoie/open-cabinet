import { AfterViewInit, Component, ElementRef, EventEmitter, Input, NgZone, OnChanges, OnDestroy, Output, ViewChild } from "@angular/core";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";

import { Cabinet, CabinetTemplate, Design } from "../model/design";
import { cabinetCentre, layoutWalls, WallPlacement } from "../room-layout";

const MM = 1 / 1000;
const WALL_THICKNESS_MM = 100;
const FRONT_THICKNESS_MM = 19;
const FRONT_GAP_MM = 3;
const TOE_KICK_SETBACK_MM = 75;
const DOUBLE_DOOR_MIN_WIDTH_MM = 609.6;
const CLICK_TOLERANCE_PX = 4;

export interface CabinetMove {
  cabinetId: string;
  offsetMm: number;
  done: boolean;
}

interface Drag {
  cabinetId: string;
  placement: WallPlacement;
  grabAlongMm: number;
}

/**
 * The Room and its Cabinets in 3D. Cabinets are picked by clicking and
 * dragged along their Wall; the editor applies the move and the Construction
 * Rules, then hands back the updated design.
 */
@Component({
  selector: "room-view",
  standalone: true,
  imports: [],
  styleUrls: ["./room-view.component.css"],
  templateUrl: "./room-view.component.html",
})
export class RoomViewComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild("canvas", { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  @Input() design!: Design;
  @Input() templates: CabinetTemplate[] = [];
  @Input() selectedCabinetId: string | null = null;
  @Input() invalidCabinetIds = new Set<string>();
  @Input() toeKickHeightMm = 101.6;

  @Output() selectCabinet = new EventEmitter<string | null>();
  @Output() moveCabinet = new EventEmitter<CabinetMove>();

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
    const templates = new Map(this.templates.map(template => [template.code, template]));

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

    for (const cabinet of this.design.cabinets) {
      const placement = placementsById.get(cabinet.wallId);
      const template = templates.get(cabinet.templateCode);
      if (!placement || !template) {
        continue;
      }
      const highlight = this.invalidCabinetIds.has(cabinet.id)
        ? colors.invalid
        : cabinet.id === this.selectedCabinetId ? colors.selected : null;
      this.content.add(this.cabinetMesh(cabinet, template, placement, colors, highlight));
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
   * A Cabinet as a group in its own frame: x along the Wall, y up, z out of
   * the Wall towards its front.
   */
  private cabinetMesh(cabinet: Cabinet, template: CabinetTemplate, placement: WallPlacement, colors: Colors, highlight: string | null): THREE.Group {
    const group = new THREE.Group();
    const centre = cabinetCentre(cabinet, placement);
    group.position.set(centre.x * MM, cabinet.elevationMm * MM, centre.z * MM);
    group.rotation.y = -placement.angleRadians;

    const base = template.type === "BASE";
    const boxBottom = base ? this.toeKickHeightMm : 0;
    const boxHeight = cabinet.heightMm - boxBottom;
    const boxDepth = cabinet.depthMm - FRONT_THICKNESS_MM;

    const carcass = box(cabinet.widthMm, boxHeight, boxDepth, colors.carcass);
    carcass.position.set(0, (boxBottom + boxHeight / 2) * MM, -FRONT_THICKNESS_MM / 2 * MM);
    group.add(carcass);

    if (base && boxBottom > 0) {
      const toeKick = box(cabinet.widthMm, boxBottom, boxDepth - TOE_KICK_SETBACK_MM, colors.toeKick);
      toeKick.position.set(0, boxBottom / 2 * MM, -(FRONT_THICKNESS_MM + TOE_KICK_SETBACK_MM) / 2 * MM);
      group.add(toeKick);
    }

    for (const front of fronts(cabinet, template, boxHeight)) {
      const mesh = box(front.width, front.height, FRONT_THICKNESS_MM, colors.front);
      mesh.position.set(front.x * MM, (boxBottom + front.y) * MM, (cabinet.depthMm / 2 - FRONT_THICKNESS_MM / 2) * MM);
      group.add(mesh);
    }

    if (highlight) {
      const outline = new THREE.LineSegments(
        new THREE.EdgesGeometry(new THREE.BoxGeometry(cabinet.widthMm * MM, cabinet.heightMm * MM, cabinet.depthMm * MM)),
        new THREE.LineBasicMaterial({ color: highlight }),
      );
      outline.position.set(0, cabinet.heightMm / 2 * MM, 0);
      group.add(outline);
    }

    group.traverse(object => object.userData["cabinetId"] = cabinet.id);
    return group;
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
    const cabinetId = this.pick(event);
    if (!cabinetId) {
      return;
    }

    const cabinet = this.design.cabinets.find(item => item.id === cabinetId)!;
    const placement = layoutWalls(this.design.walls).find(item => item.wall.id === cabinet.wallId);
    const along = placement ? this.alongWall(event, placement) : null;
    this.ngZone.run(() => this.selectCabinet.emit(cabinetId));

    if (placement && along !== null) {
      this.drag = { cabinetId, placement, grabAlongMm: along - cabinet.offsetMm };
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
      const move = { cabinetId: this.drag.cabinetId, offsetMm: along - this.drag.grabAlongMm, done: false };
      this.ngZone.run(() => this.moveCabinet.emit(move));
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
        const move = { cabinetId: drag.cabinetId, offsetMm: along - drag.grabAlongMm, done: true };
        this.ngZone.run(() => this.moveCabinet.emit(move));
      }
    } else if (this.pointerDown
      && Math.hypot(event.clientX - this.pointerDown.x, event.clientY - this.pointerDown.y) < CLICK_TOLERANCE_PX
      && !this.pick(event)) {
      this.ngZone.run(() => this.selectCabinet.emit(null));
    }
    this.pointerDown = null;
  }

  private pick(event: PointerEvent): string | null {
    this.aim(event);
    const hit = this.raycaster.intersectObjects(this.content.children, true)
      .find(intersection => intersection.object.userData["cabinetId"]);
    return hit ? hit.object.userData["cabinetId"] as string : null;
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
  };
}

function box(widthMm: number, heightMm: number, depthMm: number, color: string): THREE.Mesh {
  return new THREE.Mesh(
    new THREE.BoxGeometry(widthMm * MM, heightMm * MM, depthMm * MM),
    new THREE.MeshStandardMaterial({ color }),
  );
}

/**
 * Door and drawer fronts in the Cabinet's frame: x is the centre along the
 * Wall, y the centre above the box bottom.
 */
function fronts(cabinet: Cabinet, template: CabinetTemplate, boxHeight: number): { x: number; y: number; width: number; height: number }[] {
  const result: { x: number; y: number; width: number; height: number }[] = [];

  if (template.drawerCount > 0) {
    const height = (boxHeight - FRONT_GAP_MM * template.drawerCount) / template.drawerCount;
    for (let index = 0; index < template.drawerCount; index++) {
      result.push({ x: 0, y: FRONT_GAP_MM / 2 + index * (height + FRONT_GAP_MM) + height / 2, width: cabinet.widthMm - FRONT_GAP_MM, height });
    }
  } else if (template.doors) {
    const doors = cabinet.widthMm > DOUBLE_DOOR_MIN_WIDTH_MM ? 2 : 1;
    const width = (cabinet.widthMm - FRONT_GAP_MM * doors) / doors;
    for (let index = 0; index < doors; index++) {
      result.push({
        x: -cabinet.widthMm / 2 + FRONT_GAP_MM / 2 + index * (width + FRONT_GAP_MM) + width / 2,
        y: boxHeight / 2,
        width,
        height: boxHeight - FRONT_GAP_MM,
      });
    }
  }

  return result;
}
