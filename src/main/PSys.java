package main;

import processing.core.PGraphics;
import toxi.geom.Rect;
import toxi.physics2d.VerletMinDistanceSpring2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.VerletSpring2D;
import toxi.physics2d.behaviors.AttractionBehavior2D;
import toxi.physics2d.behaviors.ParticleBehavior2D;
import toxi.processing.ToxiclibsSupport;
import util.Color;

import java.util.ArrayList;
import java.util.List;

public class PSys {
	private final VerletPhysics2D physics;
	private final List<VerletSpring2D> springs;
	private final List<VerletSpring2D> minDistSprings;
	private final Rect bounds;

	public PSys() {
		physics = new VerletPhysics2D();
		physics.setDrag(App.DRAG);
		springs = new ArrayList<>();
		minDistSprings = new ArrayList<>();
		bounds = new Rect(350, 50, App.P5.width - 550, App.P5.height - 100);
		physics.setWorldBounds(bounds);
	}
	public void update() {
		physics.update();
		physics.setDrag(App.DRAG);
		for (VerletSpring2D s : physics.springs) { s.setStrength(App.SPR_STR); }
		for (VerletParticle2D n : physics.particles) { n.setWeight(App.NODE_WGHT); }
	}

	public void draw(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
		pg.stroke(Color.GRID);
		gfx.rect(getBounds());
		if (App.SHOW_MINDIST) drawMinDistSprings(gfx);
		if (App.SHOW_SPRINGS) drawSprings(gfx);

		if (App.SHOW_PARTICLES) {
			pg.stroke(0xff343434);
			pg.strokeWeight(1);
			drawBehaviors(gfx);
			pg.strokeWeight(1);
			pg.fill(Color.BLACK);
			drawParticles(gfx);
		}
		pg.noStroke();
		pg.noFill();
	}
	private void drawParticles(ToxiclibsSupport gfx) { for (VerletParticle2D a : physics.particles) { gfx.circle(a, 3); } }
	private void drawBehaviors(ToxiclibsSupport gfx) {
		for (ParticleBehavior2D b : physics.behaviors) {
//			AttractionBehavior2D attr = (AttractionBehavior2D) b;
			gfx.circle(((AttractionBehavior2D) b).getAttractor(), ((AttractionBehavior2D) b).getRadius());
		}
	}
	private void drawMinDistSprings(ToxiclibsSupport gfx) { for (VerletSpring2D s : minDistSprings) { gfx.line(s.a, s.b); } }
	private void drawSprings(ToxiclibsSupport gfx) { for (VerletSpring2D s : springs) { gfx.line(s.a, s.b); } }

	public void addMinDistSprings() {
		for (FSys.Node na : App.FSYS.getNodes()) {
			for (FSys.Node nb : App.FSYS.getNodes()) {
				if (na != nb) {
					float len = (na.getRadius() + nb.getRadius());
					VerletSpring2D s;
					s = new VerletMinDistanceSpring2D(na.verlet, nb.verlet, len, .01f);
					minDistSprings.add(s);
					physics.addSpring(s);
				}
			}
		}
	}
	public void clearMinDistSprings() { for (VerletSpring2D s : minDistSprings) { physics.springs.remove(s); } }
	public void rebuildMinDistSprings() { clearMinDistSprings(); addMinDistSprings(); }
	public void clearSprings() { physics.springs.clear(); }
	public void clear() {physics.clear(); springs.clear();}
	public VerletPhysics2D getPhysics() { return physics; }
	public Rect getBounds() { return bounds; }
	public void setDrag(float newDrag) { physics.setDrag(newDrag);}
	public float getDrag() {return physics.getDrag();}
}