import math
import numpy as np
import random
import sys
import json
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 
import tensorflow as tf
from PIL import Image, ImageEnhance

# Size of the Resized Image from which crops are taken
resize_img_size=256.0

# Size of the Input Image to the Network
input_img_size=224.0


def image_resize(img):

    '''
    
    Resizes img to an image of size (resize_img_size, resize_img_size)
    
    Parameters:
    img (PIL Image Object) : Image to be resized
    
    Returns:
    new_img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)

    '''

    width, height=img.size
    new_width=0
    new_height=0
    
    if width<height:
        new_width=resize_img_size
        new_height=resize_img_size*(height/width)
    
    else:
        new_height=resize_img_size
        new_width=resize_img_size*(width/height)
    
    new_size=(math.floor(new_width), math.floor(new_height))
    new_img=img.resize(new_size)
    
    return new_img


def image_crop_upper_left(img):

    '''
    
    Crops the Upper Left Part of img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    cropped_img (PIL Image Object) : Image equivalent to Upper Left Crop of img
    
    '''

    (left,upper,right,lower)=(0,0,input_img_size,input_img_size)
    cropped_img=img.crop((left,upper,right,lower))
    
    return cropped_img


def image_crop_upper_right(img):

    '''
    
    Crops the Upper Right Part of img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    cropped_img (PIL Image Object) : Image equivalent to Upper Right Crop of img
    
    '''

    width,_ =img.size
    (left,upper,right,lower)=(width-input_img_size,0,width,input_img_size)
    cropped_img=img.crop((left,upper,right,lower))
    
    return cropped_img


def image_crop_lower_left(img):

    '''
    
    Crops the Lowwer Left Part of img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    cropped_img (PIL Image Object) : Image equivalent to Lower Left Crop of img
    
    '''

    _,height=img.size
    (left,upper,right,lower)=(0,height-input_img_size,input_img_size,height)
    cropped_img=img.crop((left,upper,right,lower))
    
    return cropped_img


def image_crop_lower_right(img):
    
    '''
    
    Crops the Lower Right Part of img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    cropped_img (PIL Image Object) : PIL Image Object equivalent to Lower Right Crop of img
    
    '''

    width, height =img.size
    (left,upper,right,lower)=(width-input_img_size, height-input_img_size, width, height)
    cropped_img=img.crop((left,upper,right,lower))
    
    return cropped_img


def image_crop_central(img):
    
    '''
    
    Crops the Central Part of img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    cropped_img (PIL Image Object) : Image equivalent to Central Crop of img

    '''

    width,height=img.size
    x=(width-input_img_size)/2
    y=(height-input_img_size)/2
    (left,upper,right,lower)=(x, y, x+input_img_size, y+input_img_size)
    cropped_img=img.crop((left,upper,right,lower))
    
    return cropped_img


def image_crop(img):
    
    '''
    
    Performs and Returns all kinds of Crops on img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    upper_left (PIL Image Object) : Upper Left Crop of img
    upper_right (PIL Image Object) : Upper Right Crop of img
    lower_left (PIL Image Object) : Lower Left Crop of img
    lower_right (PIL Image Object) : Lower Right Crop of img
    central (PIL Image Object) : Central Crop of img
    
    '''

    upper_left=image_crop_upper_left(img)
    upper_right=image_crop_upper_right(img)
    lower_left=image_crop_lower_left(img)
    lower_right=image_crop_lower_right(img)
    central=image_crop_central(img)
    
    return upper_left, upper_right, lower_left, lower_right, central


def flip_horizontal(img):

    '''
    
    Flips img horizontally
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    flipped_img (PIL Image Object) : Image equivalent to img flipped horizontally
    
    '''

    flipped_img=img.transpose(Image.FLIP_LEFT_RIGHT)
    return flipped_img


def flip_vertical(img):
    
    '''
    
    Flips img vertically
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    flipped_img (PIL Image Object) : Image equivalent to img flipped vertically
    
    '''

    flipped_img=img.transpose(Image.FLIP_TOP_BOTTOM)
    return flipped_img


def apply_random_brightness(img):

    '''
    
    Applies Random Brightness to img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    bright_img (PIL Image Object) : Image that arrises by applying random brightness to img

    '''
    
    brightness=ImageEnhance.Brightness(img)
    val=round(random.uniform(0,2),1)
    bright_img=brightness.enhance(val)
    
    return bright_img


def apply_random_contrast(img):
    
    '''
    
    Applies Random Contrast to img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    bright_img (PIL Image Object) : Image that arrises by applying random contrast to img
    
    '''

    contrast=ImageEnhance.Contrast(img)
    val=round(random.uniform(0,2),1)
    contrast_img=contrast.enhance(val)
    
    return contrast_img


def apply_random_sharpness(img):

    '''
    
    Applies Random Sharpness to img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    bright_img (PIL Image Object) : Image that arrises by applying random sharpness to img
    
    '''

    sharpness=ImageEnhance.Sharpness(img)
    val=round(random.uniform(0,2),1)
    sharp_img=sharpness.enhance(val)
    
    return sharp_img


def apply_random_colour(img):

    '''
    
    Applies Random Colour to img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    bright_img (PIL Image Object) : Image that arrises by applying random colour to img
    
    '''

    colour=ImageEnhance.Color(img)
    val=round(random.uniform(0,2),1)
    colour_img=colour.enhance(val)
    
    return colour_img


def apply_random_effects(img, idx):

    '''
    
    Applies Random Effect on img depending on idx
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    idx: Index of the function to be applied
    
    Returns:
    functions[idx](img) (PIL Image Object) : Applies function[idx] on img
    
    '''
    
    functions={
        0:flip_horizontal,
        1:flip_vertical,
        2:apply_random_brightness,
        3:apply_random_contrast,
        4:apply_random_sharpness,
        5:apply_random_colour
    }

    return functions[idx](img)


def get_random_images(img,return_two=False):

    '''
    
    Applies Random Effects on img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    return_two: Boolean specifying if two images are to be returned, Default Value is False
    
    Returns:
    random_img_1: Random Effect applied on img
    random_img_2: None if return_two is false else Another Random Effect applied on img
    
    '''

    n_functions=5
    idx1=random.randint(0,n_functions)
    random_img_1=apply_random_effects(img, idx1)
    random_img_2=None
    
    if return_two:
        idx2=(idx1+random.randint(0,n_functions-1))%n_functions
        random_img_2=apply_random_effects(img,idx2)
    
    return random_img_1, random_img_2


def image_augmentation(img):

    '''
    
    Applies Several Image Augmentation Techniques on img
    
    Parameters:
    img (PIL Image Object) : Image of size (resize_img_size, resize_img_size)
    
    Returns:
    images (List of PIL Image Object) : List of Augmented Images
    
    '''

    img=image_resize(img)
    
    upper_left, upper_right, lower_left, lower_right, central=image_crop(img)    
    
    images=[]
    
    if upper_left.size==(input_img_size,input_img_size):
        images.append(upper_left)
    if upper_right.size==(input_img_size,input_img_size):
        images.append(upper_right)
    if lower_left.size==(input_img_size,input_img_size):
        images.append(lower_left)
    if lower_right.size==(input_img_size,input_img_size):
        images.append(lower_right)
    if central.size==(input_img_size,input_img_size):
        images.append(central)
        random_img_1, _ = get_random_images(central)
        images.append(random_img_1)
      
    return images


def load_model():
    return tf.keras.models.load_model("data/plant_leaf_model")


''' 
idx1="plant_name"

idx2="disease_category"

idx3="disease_name"
'''

def rotate_image(img):
    img1=img.rotate(90)
    img2=img.rotate(180)
    img3=img.rotate(270)
    return [img,img1,img2,img3]


def convert_from_categorical(Y):

    '''
    
    Convert from Categorical to Normal
    
    Arguments:
    Y: Label in categorical form
    
    Returns:
    Labels in non-categorical form
    
    '''

    return np.argmax(Y,axis=-1)


def predict_values(model, images, idx1, idx2, idx3):

    path="/home/aayussss2101/Desktop/Leaf Disease/data/num_to_label_file.json"

    with open(path) as json_file:
        num_to_label=json.load(json_file)

        predictions=model.predict(images)

        pred1=convert_from_categorical(predictions[idx1]).tolist()
        pred2=convert_from_categorical(predictions[idx2]).tolist()
        pred3=convert_from_categorical(predictions[idx3]).tolist()

        m1=max(set(pred1),key=pred1.count)
        m2=max(set(pred2),key=pred2.count)
        m3=max(set(pred3),key=pred3.count)

        return {idx1:num_to_label[0].get(str(m1),'-'),idx2:num_to_label[1].get(str(m2),'-'),idx3:num_to_label[2].get(str(m3),'-')}


def predict(img_path, idx1, idx2, idx3):

    img=Image.open(img_path)
    imgs=rotate_image(img)
    model=load_model()
    bar=[]
    for im in imgs:
        foo=image_augmentation(im)
        bar.extend(foo)

    images=[]

    for i in bar:
        im=np.array(i)
        images.append(im)

    images=np.array(images)

    return predict_values(model,images,idx1,idx2,idx3)
    

if __name__=="__main__":
    
    img_path=sys.argv[1]
    idx1=sys.argv[2]
    idx2=sys.argv[3]
    idx3=sys.argv[4]
    
    prediction={}
    
    try:
        prediction=predict(img_path,idx1,idx2,idx3)
    except Exception as e:
        prediction={idx1:'-',idx2:'-',idx3:'-'}
        print(e,file=sys.stderr)
        
    json_ob=json.dumps(prediction)
    print(json_ob)

    if os.path.exists(img_path):
        os.remove(img_path)
        