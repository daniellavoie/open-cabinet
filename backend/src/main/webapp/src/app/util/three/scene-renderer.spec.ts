import * as THREE from "three";

import { createRenderer } from "./scene-renderer";

describe("scene renderer", () => {
  const getContext = HTMLCanvasElement.prototype.getContext;

  // The test browser has no canvas at all: give it no WebGL, as a browser without hardware acceleration.
  beforeEach(() => HTMLCanvasElement.prototype.getContext = (() => null) as typeof getContext);
  afterEach(() => HTMLCanvasElement.prototype.getContext = getContext);

  it("draws as SVG beside a hidden canvas when the browser gives no WebGL", () => {
    const host = document.createElement("div");
    const canvas = document.createElement("canvas");
    host.appendChild(canvas);
    const scene = new THREE.Scene();
    scene.add(new THREE.DirectionalLight(0xffffff, 1.4));

    const renderer = createRenderer(canvas, scene);
    renderer.setSize(400, 300);
    renderer.render(scene, new THREE.PerspectiveCamera());

    expect(renderer.software).toBe(true);
    expect(renderer.domElement.tagName.toLowerCase()).toBe("svg");
    expect(host.lastElementChild).toBe(renderer.domElement);
    expect(canvas.style.display).toBe("none");
    expect(scene.children.some(child => child instanceof THREE.AmbientLight)).toBe(true);

    renderer.dispose();
    expect(host.querySelector("svg")).toBeNull();
    expect(canvas.style.display).toBe("");
    expect(scene.children.some(child => child instanceof THREE.AmbientLight)).toBe(false);
  });
});
