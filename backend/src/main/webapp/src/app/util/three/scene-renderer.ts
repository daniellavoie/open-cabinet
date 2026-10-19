import * as THREE from "three";
import { SVGRenderer } from "three/examples/jsm/renderers/SVGRenderer.js";

/**
 * Draws a 3D view: with WebGL when the browser has it, else as SVG on the
 * CPU. Browsers with hardware acceleration turned off often give no WebGL;
 * the view then stays usable, with flatter shading and slower redraws.
 */
export interface SceneRenderer {
  /** The element drawn on, which receives the pointer events. */
  readonly domElement: HTMLElement | SVGElement;
  /** Drawn on the CPU: redraw only when something changed. */
  readonly software: boolean;
  setSize(width: number, height: number): void;
  render(scene: THREE.Scene, camera: THREE.Camera): void;
  dispose(): void;
}

const SVG_AMBIENT = 0.5;
const SVG_DIRECTIONAL_INTENSITY = 0.6;

/** Largest side of the tiles boxes are cut into without a depth buffer, in scene units (metres). */
const SVG_TILE = 0.15;

const TILED = "svgTiled";

/** Marks a mesh drawn under everything else when there is no depth buffer, such as the floor. */
export const GROUND = "ground";

let webgl: boolean | undefined;

/** Whether the browser gives WebGL, asked once per page. */
function hasWebgl(): boolean {
  webgl ??= !!document.createElement("canvas").getContext("webgl2");
  return webgl;
}

export function createRenderer(canvas: HTMLCanvasElement, scene: THREE.Scene): SceneRenderer {
  if (!hasWebgl()) {
    return svgRenderer(canvas, scene);
  }
  try {
    const renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    renderer.setPixelRatio(window.devicePixelRatio);
    return {
      domElement: canvas,
      software: false,
      setSize: (width, height) => renderer.setSize(width, height, false),
      render: (scene, camera) => renderer.render(scene, camera),
      dispose: () => renderer.dispose(),
    };
  } catch {
    return svgRenderer(canvas, scene);
  }
}

function svgRenderer(canvas: HTMLCanvasElement, scene: THREE.Scene): SceneRenderer {
  const renderer = new SVGRenderer();
  const svg = renderer.domElement;
  svg.style.display = "block";
  svg.style.width = "100%";
  svg.style.height = "100%";
  svg.style.touchAction = "none";
  canvas.style.display = "none";
  canvas.after(svg);
  // The SVG renderer ignores hemisphere light, counts ambient light by its colour alone (not its
  // intensity) and adds light without tone mapping: a grey ambient light stands in for the hemisphere,
  // and directional light is dimmed, so pale surfaces keep their shade.
  const ambient = new THREE.AmbientLight(new THREE.Color(SVG_AMBIENT, SVG_AMBIENT, SVG_AMBIENT));
  scene.add(ambient);
  return {
    domElement: svg,
    software: true,
    setSize: (width, height) => renderer.setSize(width, height),
    render: (scene, camera) => {
      scene.traverse(object => {
        if (object instanceof THREE.DirectionalLight) {
          object.intensity = SVG_DIRECTIONAL_INTENSITY;
        }
        if (object instanceof THREE.Mesh) {
          tile(object);
        }
      });
      renderInLayers(renderer, scene, camera);
    },
    dispose: () => {
      scene.remove(ambient);
      svg.remove();
      canvas.style.display = "";
    },
  };
}

/**
 * The SVG renderer has no depth buffer: it paints triangles from the farthest
 * centre to the nearest, and a large floor whose centre lies near the camera
 * would cover what stands on it. What is marked ground is painted first, on
 * its own, then everything else over it.
 */
function renderInLayers(renderer: SVGRenderer, scene: THREE.Scene, camera: THREE.Camera) {
  const ground: THREE.Object3D[] = [];
  const drawn: THREE.Object3D[] = [];
  scene.traverse(object => {
    if (object.userData[GROUND]) {
      ground.push(object);
    } else if (object instanceof THREE.Mesh || object instanceof THREE.LineSegments || object instanceof THREE.Line) {
      drawn.push(object);
    }
  });
  if (ground.length === 0) {
    renderer.render(scene, camera);
    return;
  }

  const shown = (objects: THREE.Object3D[], visible: boolean) => {
    const before = objects.map(object => object.visible);
    objects.forEach(object => object.visible = visible && object.visible);
    return () => objects.forEach((object, index) => object.visible = before[index]);
  };

  const restoreDrawn = shown(drawn, false);
  renderer.autoClear = true;
  renderer.render(scene, camera);
  restoreDrawn();

  const restoreGround = shown(ground, false);
  const background = scene.background;
  scene.background = null;
  renderer.autoClear = false;
  renderer.render(scene, camera);
  scene.background = background;
  restoreGround();
}

/**
 * Cuts an opaque box into small tiles, once: painted from the farthest centre
 * to the nearest, a large face (a shelf, a side) can sort in front of what
 * stands before it, while small tiles sort as they should. See-through boxes
 * (walls, fronts shown open) stay whole: their tiles' overlaps would show as
 * a darker grid.
 */
function tile(mesh: THREE.Mesh) {
  if (mesh.userData[TILED] || !(mesh.geometry instanceof THREE.BoxGeometry) || seeThrough(mesh.material)) {
    return;
  }
  const { width, height, depth } = mesh.geometry.parameters;
  const segments = (size: number) => Math.max(1, Math.ceil(size / SVG_TILE));
  const tiled = new THREE.BoxGeometry(width, height, depth, segments(width), segments(height), segments(depth));
  mesh.geometry.dispose();
  mesh.geometry = tiled;
  mesh.userData[TILED] = true;
}

function seeThrough(material: THREE.Material | THREE.Material[]): boolean {
  return (Array.isArray(material) ? material : [material]).some(item => item.transparent && item.opacity < 1);
}
