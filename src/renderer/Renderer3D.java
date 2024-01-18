package renderer;

import model3D.Object3D;
import model3D.Scene;
import rasterizer.Raster;
import rasterizer.line.Line;
import transforms.Mat4;
import transforms.Point3D;
import transforms.Vec2D;

import java.util.ArrayList;
import java.util.List;

public class Renderer3D {
    public void render(final Raster raster,
                       final Scene scene,
                       final Line line,
                       final Mat4 viewMatrix,
                       final Mat4 projectMatrix){
        //for each object 3d in scene
        final Mat4 vp = viewMatrix.mul(projectMatrix);
        for (Object3D object: scene.getObjects()) {
            if(object.isHidden()){
                continue;
            }
            Mat4 T =  object.getModelMat().mul(vp);

            List<Point3D> trasformedPoints = new ArrayList<>();
            //   transformed points = p -> p * T
            for (Point3D point: object.getVertexBuffer()) {
                Point3D result = point.mul(T);
                trasformedPoints.add(result);
            }
            //   Lines l = from index buffer

            for(int i = 0; i < object.getIndexBuffer().size(); i += 2){
                int indexA =  object.getIndexBuffer().get(i);
                int indexB =  object.getIndexBuffer().get(i + 1);

                Point3D start =  trasformedPoints.get(indexA);
                Point3D end   = trasformedPoints.get(indexB);

                //   for each rasterizer.line
                if( isClipped(start) || isClipped(end)){
                    continue;
                }
                //          dehomog -> Point3D -> Vec3D
                start.dehomog().ifPresent(
                    p1 -> end.dehomog().ifPresent(
                        p2 -> {
                        //    projection to 2D -> Vec2D (ignore z)
                            Vec2D start2D = p1.ignoreZ();
                            Vec2D end2D = p2.ignoreZ();
                        //   transform to viewport
                            start2D =  transformToViewport(start2D);
                            end2D   = transformToViewport(end2D);
                        //   line rasterize
                            line.drawLine(raster, start2D.getX(), start2D.getY(),
                                    end2D.getX(), end2D.getY(),
                                    object.getColor());
                }));
            }
        }
    }

    private boolean isClipped(Point3D p){
        return ( !isInRange(p));
    }
    private boolean isInRange(Point3D p){
        return p.getX() >= -p.getW() && p.getX() <= p.getW()
            && p.getY() >= -p.getW() && p.getY() <= p.getW()
            && p.getZ() >= 0 && p.getZ() <= p.getW();
    }
    /**
     * Transfromation to viewport in 2D
     * @param v Vec2D
     * @return transformed vector
     */
    private Vec2D transformToViewport(Vec2D v) {
        return v.mul(new Vec2D(1, -1)) // here -1 for y because we change the smer of Oy
                .add(new Vec2D(1, 1))
                .mul(new Vec2D((800 - 1) / 2., (600 - 1) / 2.)); //
        // /2. because we move <-1;1> -> <0; 2> and then this 2 is not needed
    }

}

