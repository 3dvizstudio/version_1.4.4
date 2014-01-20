package main;

import processing.core.PGraphics;
import toxi.geom.Circle;
import toxi.geom.Rect;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.VerletSpring2D;
import toxi.physics2d.behaviors.AttractionBehavior2D;
import toxi.physics2d.behaviors.ParticleBehavior2D;
import toxi.processing.ToxiclibsSupport;

import java.util.ArrayList;
import java.util.List;

public class PSys {
	private final VerletPhysics2D physics;
	private final List<AttractionBehavior2D> attractors;
	private final List<VerletSpring2D> springs;
	private final Rect bounds;
	private VerletParticle2D activeParticle;
	private List<VerletParticle2D> selectedParticles;
	private float separation = 20;

	public PSys() {
		physics = new VerletPhysics2D();
		physics.setDrag(App.DRAG);
		attractors = new ArrayList<>();
		springs = new ArrayList<>();
		bounds = new Rect(350, 50, 1200, 800);
		physics.setWorldBounds(bounds);
		selectedParticles = new ArrayList<>();
	}

	public void update() {
		physics.update();
		physics.setDrag(App.DRAG);
		for (AttractionBehavior2D a : attractors) { a.setRadius(App.ATTR_RAD); a.setStrength(App.ATTR_STR); }
		for (VerletSpring2D s : physics.springs) { s.setStrength(App.SPR_STR); }
		for (VerletParticle2D n : physics.particles) { n.setWeight(App.NODE_WGHT); }
	}
	public void draw(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		pg.stroke(0xff222222);
		gfx.rect(getBounds());
		pg.stroke(0xff507f81);
		for (VerletSpring2D s : physics.springs) { gfx.line(s.a, s.b); }
		pg.stroke(0xff663344);
		for (VerletSpring2D s : springs) { gfx.line(s.a, s.b); }
		pg.stroke(0xff666666);
		for (VerletParticle2D a : physics.particles) {
			gfx.circle(a, a.getWeight());
		} pg.stroke(0xff333333);
		for (ParticleBehavior2D b : physics.behaviors) {
			AttractionBehavior2D ba = (AttractionBehavior2D) b;
			float b_rad = ba.getRadius();
			gfx.circle(ba.getAttractor(), b_rad);
			pg.fill(0xff222222);
			pg.text("b_rad", ba.getAttractor().x + b_rad, ba.getAttractor().y - 5);
			pg.text(b_rad, ba.getAttractor().x + b_rad, ba.getAttractor().y + 5);
			pg.noFill();
		}
		if (activeParticle != null) { pg.stroke(0xff8b6714); gfx.circle(activeParticle, 14); }
		if (!selectedParticles.isEmpty()) { pg.stroke(0xff8d5427); for (VerletParticle2D v : selectedParticles) { gfx.circle(v, 10); } }
		pg.noStroke();
	}

	public void addParticle(Vec2D pos, float separation) {
		VerletParticle2D p = new VerletParticle2D(pos);
		physics.addParticle(p);
		physics.addBehavior(new AttractionBehavior2D(p, 2 * separation, 1.2f));
	}

	public void selectParticleNearPosition(Vec2D mousePos) {
		Circle c = new Circle(mousePos, 20);
		deselectParticle();
		for (VerletParticle2D a : physics.particles) {
			if (c.containsPoint(a)) {
				setActiveParticle(a);
				activeParticle.lock();
				if (App.isShiftDown) { selectedParticles.add(a); } else {selectedParticles.clear(); selectedParticles.add(a);}
				break;
			}
		} if ((activeParticle == null) && (!App.isShiftDown)) { selectedParticles.clear(); }
	}
	public void moveActiveParticle(Vec2D mousePos) {
		if (activeParticle != null) { activeParticle.set(mousePos); }
	}
	public void deselectParticle() {if (hasActiveParticle()) {activeParticle.unlock();} activeParticle = null;}
	private void setActiveParticle(VerletParticle2D a) { activeParticle = a; activeParticle.lock(); }
	public boolean hasActiveParticle() { return activeParticle != null; }
	public VerletParticle2D getActiveParticle() { return activeParticle; }
	public List<VerletParticle2D> getSelectedParticles() { return selectedParticles; }

	public VerletPhysics2D getPhysics() { return physics; }
	public Rect getBounds() { return bounds; }
	public void setDrag(float newDrag) { physics.setDrag(newDrag);}
	public void setSeparation(float s) {
		separation = s;
		for (ParticleBehavior2D p : physics.behaviors) {
			if (!attractors.contains(p)) { AttractionBehavior2D a = (AttractionBehavior2D) p; a.setRadius(separation); }
		}
	}
	public float getSeparation() { return separation; }
	public float getDrag() {return physics.getDrag();}
	public void clear() { deselectParticle(); physics.clear(); selectedParticles.clear(); springs.clear();}
	public void clearSprings() { physics.springs.clear(); }
}
