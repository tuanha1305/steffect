#ifndef __SHADERS_H__
#define __SHADERS_H__

#define SHADER_STRING(...) #__VA_ARGS__
const char* const g_mouseVerShader = SHADER_STRING
(
        uniform mat4 projection;
        uniform mat4 modelView;
        attribute vec4 vPosition;
        attribute vec4 SourceColor;
        varying vec4 DestinationColor;
        void main(void)
        {
            DestinationColor = SourceColor;
            gl_Position =  vPosition;
        }
);

const char* const g_mouseFraShader = SHADER_STRING
(
        precision mediump float;
        uniform highp float color_selector;
        varying lowp vec4 DestinationColor;
        void main()
        {
            gl_FragColor =  vec4(DestinationColor.rgb, DestinationColor.a * 0.2);
        }
);

const char *const ChangeFaceSizeVsh = SHADER_STRING
(
        attribute vec4 position;
        attribute vec4 inputTextureCoordinate;
        varying highp vec2 textureCoordinate;

        void main(void)
        {
            gl_Position =  position;
            textureCoordinate = inputTextureCoordinate.xy;
        }


);

const char *const ChangeFaceSizeFsh = SHADER_STRING
(
// 瘦脸
        precision highp float;

        varying highp vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;

        uniform highp float radius;

        uniform highp float aspectRatio;

        uniform float leftContourPoints[9*2];
        uniform float rightContourPoints[9*2];
        uniform float deltaArray[9];
        uniform int arraySize;

        highp vec2 warpPositionToUse(vec2 currentPoint, vec2 contourPointA,  vec2 contourPointB, float radius, float delta, float aspectRatio)
{
    vec2 positionToUse = currentPoint;

//    vec2 currentPointToUse = vec2(currentPoint.x, currentPoint.y * aspectRatio + 0.5 - 0.5 * aspectRatio);
//    vec2 contourPointAToUse = vec2(contourPointA.x, contourPointA.y * aspectRatio + 0.5 - 0.5 * aspectRatio);

    vec2 currentPointToUse = currentPoint;
    vec2 contourPointAToUse = contourPointA;

    float r = distance(currentPointToUse, contourPointAToUse);
    if(r < radius)
    {
        vec2 dir = normalize(contourPointB - contourPointA);
        float dist = radius * radius - r * r;
        float alpha = dist / (dist + (r-delta) * (r-delta));
        alpha = alpha * alpha;

        positionToUse = positionToUse - alpha * delta * dir;

    }

    return positionToUse;
}

        void main()
        {
            vec2 positionToUse = textureCoordinate;

            for(int i = 0; i < 9; i++)
            {
                positionToUse = warpPositionToUse(positionToUse, vec2(leftContourPoints[i * 2], leftContourPoints[i * 2 + 1]), vec2(rightContourPoints[i * 2], rightContourPoints[i * 2 + 1]), radius, deltaArray[i], aspectRatio);
                positionToUse = warpPositionToUse(positionToUse, vec2(rightContourPoints[i * 2], rightContourPoints[i * 2 + 1]), vec2(leftContourPoints[i * 2], leftContourPoints[i * 2 + 1]), radius, deltaArray[i], aspectRatio);
            }


            gl_FragColor = texture2D(inputImageTexture, positionToUse);

        }
);

const char* const vertexShaderCode =SHADER_STRING(
        attribute vec4 position;
        attribute vec4 inputTextureCoordinate;
        varying vec2 textureCoordinate;
        void main()
        {
            gl_Position = position;
            textureCoordinate = inputTextureCoordinate.xy;
        }
);

const char* const fragmentShaderCode =SHADER_STRING(
        precision mediump float;
        varying highp vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        void main()
        {
            vec4 texColor = texture2D(inputImageTexture, textureCoordinate);
            if(texColor.a <0.01){
                discard;
            }
            gl_FragColor = texColor;
        }
);


const char *const ChangeFaceAndJawV = SHADER_STRING
(
//贴图渲染

        attribute vec2 position;
        attribute vec2 inputTextureCoordinate;
        varying  vec2 textureCoordinate;

        void main(void)
        {
            gl_Position =  vec4(position.xy, 0, 1.0);
            textureCoordinate = inputTextureCoordinate;
            //    textureCoordinate = vec2(inputTextureCoordinate.x*2.0-1.0, inputTextureCoordinate.y*2.0-1.0);
        }


);

const char *const ChangeFaceAndJawF = SHADER_STRING
(
        precision highp float;
        varying highp vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        uniform highp float radius;
        uniform highp float aspectRatio;
        uniform float leftContourPoints[9*2];
        uniform float rightContourPoints[9*2];
        uniform float deltaFaceArray[9];

        uniform float jawContourPoints[7*2];
        uniform float jawDownPoints[7*2];
        uniform float deltaJawArray[7];

        highp vec2 warpPositionToUse(vec2 currentPoint, vec2 contourPointA, vec2 contourPointB,float radius, float delta, float aspectRatio)
{
    vec2 positionToUse = currentPoint;

    //    vec2 currentPointToUse = vec2(currentPoint.x, currentPoint.y * aspectRatio + 0.5 - 0.5 * aspectRatio);
    //    vec2 contourPointAToUse = vec2(contourPointA.x, contourPointA.y * aspectRatio + 0.5 - 0.5 * aspectRatio);

    vec2 currentPointToUse = currentPoint;
    vec2 contourPointAToUse = contourPointA;

    float r = distance(currentPointToUse, contourPointAToUse);
    if(r < radius)
    {
        vec2 dir = normalize(contourPointB - contourPointA);
        float dist = radius * radius - r * r;
        float alpha = dist / (dist + (r-delta) * (r-delta));
        alpha = alpha * alpha;

        positionToUse = positionToUse - alpha * delta * dir;
    }

    return positionToUse;
}

        void main()
        {
            vec2 positionToUse = textureCoordinate;

            for(int i = 0; i < 9; i++)
            {
                positionToUse = warpPositionToUse(positionToUse, vec2(leftContourPoints[i * 2], leftContourPoints[i * 2 + 1]), vec2(rightContourPoints[i * 2], rightContourPoints[i * 2 + 1]), radius, deltaFaceArray[i], aspectRatio);
                positionToUse = warpPositionToUse(positionToUse, vec2(rightContourPoints[i * 2], rightContourPoints[i * 2 + 1]), vec2(leftContourPoints[i * 2], leftContourPoints[i * 2 + 1]), radius, deltaFaceArray[i], aspectRatio);
            }

            for(int i = 0; i < 7; i++)
            {
                positionToUse = warpPositionToUse(positionToUse,
                                                  vec2(jawContourPoints[i * 2], jawContourPoints[i * 2 + 1]),
                                                  vec2(jawDownPoints[i * 2], jawDownPoints[i * 2 + 1]),
                                                  radius, deltaJawArray[i], aspectRatio);
            }

            gl_FragColor = texture2D(inputImageTexture, positionToUse);

        }

);


const char *const FaceLianpuV = SHADER_STRING
(
//贴图渲染

        attribute vec2 position;
        attribute vec2 inputTextureCoordinate;
        varying  vec2 textureCoordinate;

        void main(void)
        {
            gl_Position =  vec4(position.xy, 0, 1.0);
            textureCoordinate = inputTextureCoordinate;
            //    textureCoordinate = vec2(inputTextureCoordinate.x*2.0-1.0, inputTextureCoordinate.y*2.0-1.0);
        }


);

const char *const FaceLianpuF = SHADER_STRING
(
        precision highp float;
        varying highp vec2 textureCoordinate;
        uniform sampler2D inputImageTexture;
        uniform sampler2D lianpuTexture;
        uniform float facePoints[212];
        uniform float leftEyes[16];
        uniform float rightEyes[16];
        uniform float mousePoints[16];

        bool ptInPolygon(vec2 pt, float facept[212])
        {
            bool ncross = false;
            int i =0;
            int j = 0;
            for( i = 0, j = 32; i < 33; j = i++)
            {
                vec2 p1 = vec2(facept[i * 2], facept[ i * 2 + 1]);
                vec2 p2 = vec2(facept[j * 2], facept[ j * 2 + 1] );

                if( ((p1.y > pt.y) != (p2.y > pt.y)) && (pt.x < (p2.x - p1.x) * (pt.y - p1.y) / (p2.y - p1.y) + p1.x))
                    ncross = !ncross;
            }
            return ncross;
        }

        bool ptInEllipse(vec2 pt)
        {

            vec2 x1 = vec2(facePoints[0], facePoints[1]);
            vec2 x2 = vec2(facePoints[32 * 2], facePoints[32 * 2 + 1]);
            float a2 = distance(x1, x2) / 2.0;
            vec2 y1 = vec2(facePoints[43 * 2], facePoints[ 43 * 2 + 1]);
            vec2 y2 = vec2(facePoints[87 * 2], facePoints[ 87 * 2 + 1]);
            float b2 = distance(y1, y2) / 2.0;
            float c = sqrt(a2 * a2 - b2 * b2 );
            vec2 center = vec2((x1.x + x2.x)/2.0, (x1.y + x2.y) / 2.0);

            float degress = atan(x2.y - x1.y, x2.x - x1.x);
            float dx = c * cos(degress);
            float dy = c * sin(degress);

            vec2 p1 = vec2(center.x - dx, center.y - dy);
            vec2 p2 = vec2(center.y + dx, center.y + dy);

            bool ncross = false;

//            if( distance(pt, p1) + distance( pt, p2 ) < a2 * 2.0)
//                ncross = true;
            if(distance(pt, center) < a2 )
                ncross = true;
            return ncross;
        }

        void main()
        {

            vec2 positionToUse = textureCoordinate;
            if( ptInPolygon(positionToUse, facePoints) || ptInEllipse(positionToUse))
            {
                float dx = positionToUse.x - facePoints[34 * 2];
                float dy = positionToUse.y - facePoints[34 * 2 + 1];
                vec2 coord = vec2(0.4f + dx, 0.4f + dy);
                gl_FragColor = texture2D(lianpuTexture, coord);
            }
            else
                gl_FragColor = texture2D(inputImageTexture, positionToUse);

        }

);

#endif

