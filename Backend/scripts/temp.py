import sys
import json
import os
from PIL import Image

def rotate_image(img):
    img1=img.rotate(90)
    img2=img.rotate(180)
    img3=img.rotate(270)
    return [img,img1,img2,img3]


if __name__=="__main__":
    path=sys.argv[1] 
    idx1=sys.argv[2]
    idx2=sys.argv[3]
    idx3=sys.argv[4]
    prediction={}

    try:
        img=Image.open(path)
        imgs=rotate_image(img)
        
        #img.show()
        prediction={
            idx1:"Lettuce",
            idx2:"Healthy",
            idx3:"Disease"
        }
    except Exception as e:
        prediction={idx1:'-',idx2:'-',idx3:'-'}
        print(e,file=sys.stderr)

    json_object=json.dumps(prediction)
    print(json_object)

    if os.path.exists(path):
        os.remove(path)
