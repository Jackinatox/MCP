import data from "../eggs/egg.json";

const file = Bun.file("./script.sh");

const text = await file.text();

const newData = {
    ...data,
    scripts: {
        ...data.scripts,
        installation: {
            ...data.scripts.installation,
            script: text
        }
    }
};


Bun.write("./egg.json", JSON.stringify(newData, null, 2));
