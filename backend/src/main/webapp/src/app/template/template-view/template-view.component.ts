import { AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, NgZone, OnChanges, OnDestroy, Output, ViewChild } from "@angular/core";
import * as THREE from "three";
import { OrbitControls } from "three/examples/jsm/controls/OrbitControls.js";

import { AssembledPart, CABINET } from "../../project/assembly";
import { Face } from "../../project/model/design";

const MM = 0.001;
/** A click moving less than this, in pixels, picks; more turns the view. */
const CLICK_TOLERANCE_PX = 4;

/** A face clicked in the view: of a Part (by its template name) or of the Cabinet. */
export interface PickedFace {
  to: string;
  face: Face;
}

/**
 * One Cabinet of a template drawn as its Parts, in the Cabinet's frame. A
 * click selects a Part; while attaching, it picks the face of a Part, or of
 * the Cabinet's outline, to hold the chosen face against.
 */
@Component({
  selector: "template-view",
  changeDetection: ChangeDetectionStrategy.Eager,
  standalone: true,
  imports: [],
  styleUrls: ["./template-view.component.css"],
  templateUrl: "./template-view.component.html",
})
export class TemplateViewComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild("canvas", { static: true }) canvasRef!: ElementRef<HTMLCanvasElement>;

  @Input() parts: AssembledPart[] = [];
  @Input() width = 600;
  @Input() height = 800;
  @Input() depth = 600;
  /** The template Part or front stack selected. */
  @Input() selected: string | null = null;
  /** Clicks pick a target face rather than select. */
  @Input() attaching = false;

  @Output() select = new EventEmitter<string | null>();
  @Output() pick = new EventEmitter<PickedFace>();

  private renderer?: THREE.WebGLRenderer;
  private scene = new THREE.Scene();
  private camera = new THREE.PerspectiveCamera(40, 1, 0.01, 50);
  private controls?: OrbitControls;
  private content = new THREE.Group();
  private cabinet?: THREE.Mesh;
  private raycaster = new THREE.Raycaster();
  private frame = 0;
  private resizeObserver?: ResizeObserver;
  private framed = "";
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
      sun.position.set(3, 6, 5);
      this.scene.add(sun);
      this.scene.add(this.content);

      this.controls = new OrbitControls(this.camera, canvas);
      this.controls.enableDamping = true;

      this.listen("pointerdown", event => this.pointerDown = { x: (event as PointerEvent).clientX, y: (event as PointerEvent).clientY });
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
    const style = getComputedStyle(this.host.nativeElement);
    const token = (name: string) => style.getPropertyValue(name).trim() || "#888888";
    this.scene.background = new THREE.Color(token("--oc-3d-background"));

    // The Cabinet's outline: pickable for its faces while attaching.
    this.cabinet = new THREE.Mesh(
      new THREE.BoxGeometry(this.width * MM, this.height * MM, this.depth * MM),
      new THREE.MeshBasicMaterial({ color: token("--oc-3d-selected"), transparent: true, opacity: this.attaching ? 0.06 : 0, depthWrite: false }),
    );
    this.cabinet.position.set(0, this.height / 2 * MM, 0);
    this.content.add(this.cabinet);
    const edges = new THREE.LineSegments(
      new THREE.EdgesGeometry(new THREE.BoxGeometry(this.width * MM, this.height * MM, this.depth * MM)),
      new THREE.LineBasicMaterial({ color: token("--oc-3d-toe-kick") }),
    );
    edges.position.copy(this.cabinet.position);
    this.content.add(edges);

    for (const part of this.parts) {
      const { x0, x1, y0, y1, z0, z1 } = part.box;
      const color = part.source === this.selected ? token("--oc-3d-selected")
        : part.front ? token("--oc-3d-front") : token("--oc-3d-carcass");
      const mesh = new THREE.Mesh(
        new THREE.BoxGeometry(Math.max(x1 - x0, 0.5) * MM, Math.max(y1 - y0, 0.5) * MM, Math.max(z1 - z0, 0.5) * MM),
        new THREE.MeshStandardMaterial({ color, transparent: part.front, opacity: part.front ? 0.8 : 1 }),
      );
      mesh.position.set(((x0 + x1) / 2 - this.width / 2) * MM, (y0 + y1) / 2 * MM, ((z0 + z1) / 2 - this.depth / 2) * MM);
      mesh.userData["source"] = part.source;
      this.content.add(mesh);
      const outline = new THREE.LineSegments(new THREE.EdgesGeometry(mesh.geometry), new THREE.LineBasicMaterial({ color: token("--oc-3d-toe-kick") }));
      outline.position.copy(mesh.position);
      this.content.add(outline);
    }

    const size = `${this.width}/${this.height}/${this.depth}`;
    if (size !== this.framed) {
      this.framed = size;
      const extent = Math.max(this.width, this.height, this.depth) * MM;
      this.controls!.target.set(0, this.height / 2 * MM, 0);
      this.camera.position.set(extent * 1.1, this.height * MM * 0.9 + extent * 0.6, extent * 1.9);
      this.camera.updateProjectionMatrix();
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

  private onPointerUp(event: PointerEvent) {
    const down = this.pointerDown;
    this.pointerDown = null;
    if (!down || Math.hypot(event.clientX - down.x, event.clientY - down.y) > CLICK_TOLERANCE_PX) {
      return;
    }
    const rect = this.canvasRef.nativeElement.getBoundingClientRect();
    this.raycaster.setFromCamera(new THREE.Vector2(
      ((event.clientX - rect.left) / rect.width) * 2 - 1,
      -((event.clientY - rect.top) / rect.height) * 2 + 1,
    ), this.camera);

    const parts = this.content.children.filter(child => child.userData["source"]);
    const hit = this.raycaster.intersectObjects(parts, false)[0];
    if (!this.attaching) {
      this.ngZone.run(() => this.select.emit(hit ? hit.object.userData["source"] as string : null));
      return;
    }
    const target = hit ?? this.raycaster.intersectObject(this.cabinet!, false)[0];
    const face = target?.face ? faceOf(target.face.normal) : null;
    if (target && face) {
      this.ngZone.run(() => this.pick.emit({ to: hit ? hit.object.userData["source"] as string : CABINET, face }));
    }
  }
}

/** The face a box's surface normal points out of. */
function faceOf(normal: THREE.Vector3): Face | null {
  if (Math.abs(normal.x) > 0.5) {
    return normal.x > 0 ? "RIGHT" : "LEFT";
  }
  if (Math.abs(normal.y) > 0.5) {
    return normal.y > 0 ? "TOP" : "BOTTOM";
  }
  if (Math.abs(normal.z) > 0.5) {
    return normal.z > 0 ? "FRONT" : "BACK";
  }
  return null;
}
