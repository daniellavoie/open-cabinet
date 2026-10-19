import * as THREE from "three";

import { createRenderer, GROUND } from "./scene-renderer";

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

  it("paints the ground before everything else, however near the camera its centre is", () => {
    const canvas = document.createElement("canvas");
    document.body.appendChild(canvas);
    const scene = new THREE.Scene();
    scene.add(new THREE.DirectionalLight(0xffffff, 1));
    const floor = new THREE.Mesh(new THREE.PlaneGeometry(12, 12), new THREE.MeshStandardMaterial({ color: 0xff0000 }));
    floor.rotation.x = -Math.PI / 2;
    floor.userData[GROUND] = true;
    // A cabinet far behind the floor's centre: sorted by centres alone, the floor would cover it.
    const cabinet = new THREE.Mesh(new THREE.BoxGeometry(1, 1, 1), new THREE.MeshStandardMaterial({ color: 0x0000ff }));
    cabinet.position.set(0, 0.5, -5);
    scene.add(floor, cabinet);
    const camera = new THREE.PerspectiveCamera(70, 4 / 3, 0.1, 100);
    camera.position.set(0, 8, 14);
    camera.lookAt(0, 0, -1);
    camera.updateMatrixWorld();

    const renderer = createRenderer(canvas, scene);
    renderer.setSize(400, 300);
    renderer.render(scene, camera);

    const fills = Array.from(renderer.domElement.querySelectorAll("path"))
      .map(path => path.getAttribute("style") ?? "")
      .map(style => /fill:rgb\((\d+),(\d+),(\d+)\)/.exec(style))
      .filter(match => !!match)
      .map(match => Number(match![1]) > Number(match![3]) ? "floor" : "cabinet");
    expect(fills).toContain("floor");
    expect(fills).toContain("cabinet");
    expect(fills.lastIndexOf("floor")).toBeLessThan(fills.indexOf("cabinet"));
    expect(floor.visible && cabinet.visible).toBe(true);
    renderer.dispose();
    canvas.remove();
  });
});
