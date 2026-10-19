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
      });
      renderer.render(scene, camera);
    },
    dispose: () => {
      scene.remove(ambient);
      svg.remove();
      canvas.style.display = "";
    },
  };
}
