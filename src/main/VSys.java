package main;

import processing.core.PApplet;
import processing.core.PGraphics;
import toxi.geom.Rect;
import toxi.geom.Polygon2D;
import toxi.geom.PolygonClipper2D;
import toxi.geom.SutherlandHodgemanClipper;
import toxi.geom.Vec2D;
import toxi.geom.mesh2d.Voronoi;
import toxi.physics2d.VerletParticle2D;
import toxi.processing.ToxiclibsSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static util.Color.*;

public class VSys {
	private PolygonClipper2D clipper;
	private Voronoi voronoi;
	private ArrayList<VerletParticle2D> sites;
	private ArrayList<Polygon2D> cells;
	private String drawMode = "poly";

	public VSys(PSys psys) {
		this.sites = psys.getPhysics().particles;
		this.clipper = new SutherlandHodgemanClipper(psys.getBounds());
		this.voronoi = new Voronoi();
		this.cells = new ArrayList<>();
	}
	public void setDrawMode(String drawMode) {this.drawMode = drawMode;}
	public ArrayList<Polygon2D> getCells() { return cells; }
	public List<VerletParticle2D> getSites() { return sites; }
	public Voronoi getVoronoi() { return voronoi; }

	public void update() {
		if (App.UPDATE_VORONOI) {
			voronoi = new Voronoi();
			voronoi.addPoints(sites);
//			voronoi.addPoints(App.PSYS.getPhysics().particles);
		} if (voronoi != null) {
			cells = new ArrayList<>();/* setCells(new ArrayList<Polygon2D>());*/
			HashMap<Polygon2D, Integer> cellmap = new HashMap<>();
			for (Polygon2D poly : voronoi.getRegions()) {
				poly = clipper.clipPolygon(poly);
				for (Vec2D v : this.sites) {
					if (poly.containsPoint(v)) { cells.add(poly); }
				}
			}
		}
	}
	public void draw(ToxiclibsSupport gfx) {
		PGraphics pg = gfx.getGraphics();
//		if (App.SHOW_VORONOI && getVoronoi() != null) {
		switch (drawMode) {
			case "none": break;
			case "verts": pg.stroke(VOR_VERTS); drawVorVerts(gfx); break;
			case "bezier": pg.stroke(VOR_CELLS); drawVorBezier(pg); pg.stroke(VOR_VERTS); drawVorVerts(gfx); break;
			case "poly": pg.stroke(VOR_CELLS); drawVorPoly(gfx); pg.stroke(VOR_VERTS); drawVorVerts(gfx); break;
			case "info": pg.fill(VOR_TXT); drawVorInfo(gfx, pg); break;
		} pg.noStroke(); pg.noFill();
//		}
	}
	private void drawVorVerts(ToxiclibsSupport gfx) {
		for (Polygon2D poly : cells) {
			for (Vec2D vec : poly.vertices) { gfx.circle(vec, 3); }
		}
	}
	private void drawVorBezier(PGraphics pg) {
		for (Polygon2D poly : cells) {
			List<Vec2D> vec = poly.vertices;
			int count = vec.size();
			pg.beginShape();
			pg.vertex((vec.get(count - 1).x + vec.get(0).x) / 2, (vec.get(count - 1).y + vec.get(0).y) / 2);
			for (int i = 0; i < count; i++) { pg.bezierVertex(vec.get(i).x, vec.get(i).y, vec.get(i).x, vec.get(i).y, (vec.get((i + 1) % count).x + vec.get(i).x) / 2, (vec.get((i + 1) % count).y + vec.get(i).y) / 2); }
			pg.endShape(PApplet.CLOSE);
		}
	}
	private void drawVorPoly(ToxiclibsSupport gfx) {
		for (Polygon2D poly : cells) { gfx.polygon2D(poly); }
	}
	private void drawVorInfo(ToxiclibsSupport gfx, PGraphics pg) {
		for (Polygon2D poly : cells) {
			pg.text(poly.getNumVertices() + "." + cells.indexOf(poly), poly.getCentroid().x, poly.getCentroid().y);
		}
	}
}
//	HashMap<Vec2D, Integer> sitemap = new HashMap<>();
//	public void setVoronoi(Voronoi v) { voronoi = v; }
//	public void setCells(ArrayList<Polygon2D> cells) { this.cells = cells; }
//	public void setCellmap(HashMap<Polygon2D, Integer> cellmap) { this.cellmap = cellmap; }
//	public HashMap<Vec2D, Integer> getSitemap() { return sitemap; }
//	public PolygonClipper2D getClipper() { return clipper; }
//	public List<Vec2D> getCellSites() { return sites; }
//	public void addCell(Vec2D v) { getCellSites().add(v); }
//	public void addSite(Vec2D v, Integer i) {sitemap.put(v, i);}
//	public void setClipper(PolygonClipper2D clipper) { this.clipper = clipper; }
//	public void setCellSites(ArrayList<Vec2D> sites) { this.sites = sites; }
//	public void setSitemap(HashMap<Vec2D, Integer> sitemap) { this.sitemap = sitemap; }
//	public HashMap<Polygon2D, Integer> getCellmap() { return cellmap; }
