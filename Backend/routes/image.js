let express=require('express')
let router=express.Router()
let constants=require('../constants')
let multer=require('multer')
let upload=multer({dest:'image/'})
//let jo=require('jpeg-autorotate')
//let fs=require('fs')
const spawn=require('child_process').spawn

/*function deleteFiles(){
    for(let i=0;i<arguments.length;i++){
        let path=arguments[i]
        if(fs.existsSync(path)){
            fs.unlink(path,(err)=>{
                if(err)
                    console.log(`Error in deleting file ${path} : ${err}`)
            })
        }
    }
}*/

router.post('/',upload.single('image'),function(req,res,next){

    let path=req.file.path

    //let path='image/1.png'

    /*jo.rotate(req.file.path,{},function(error,buffer,orientation){
        
        deleteFiles(orig_path)
        
        if(error){
            console.log(`Error occurred while rotating image : ${error.message}`)
            //res.send({})
        }
        console.log(`Image orientation was : ${orientation}`)
        
        fs.writeFile(path,buffer,(err)=>{
            if(err){
                console.log(err)
                //res.send({})
            }
        })
    })*/

    const pythonProcess=spawn('python',['scripts/image_preprocess.py',path,constants.idx1,constants.idx2,constants.idx3])

    let plant={}

    pythonProcess.stdout.on('data',(data)=>{
        console.log(`stdout: ${data}`)
        plant=JSON.parse(data)
        //res.send(plant)
    })

    pythonProcess.stderr.on('data',(data)=>{
        console.log(`stderr: ${data}`)
    })

    pythonProcess.on('exit',(code)=>{
        console.log(`Process quit with code ${code}`)
        console.log(plant)
        res.send(plant)
    })

    /*let plant={
        "Plant Name":"Lettuce",
        "Infection Type":"Fungal",
        "Disease Name":"Disease"
    }*/

    //res.send(plant)

});

module.exports=router